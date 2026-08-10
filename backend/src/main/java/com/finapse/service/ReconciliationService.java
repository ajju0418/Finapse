package com.finapse.service;

import com.finapse.entity.ReconciliationReview;
import com.finapse.entity.Transaction;
import com.finapse.entity.TransactionLink;
import com.finapse.enums.*;
import com.finapse.repository.ReconciliationReviewRepository;
import com.finapse.repository.TransactionLinkRepository;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Core reconciliation engine.
 *
 * Detects:
 * 1. Credit card payments — bank DEBIT matching a card statement's total/amount
 * 2. Refunds — CREDIT on same source matching a prior DEBIT (same amount, similar description)
 * 3. Cashback — small CREDIT on a card following a DEBIT (same card, within 30 days)
 *
 * Never auto-confirms. All matches go to REVIEW_REQUIRED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final int CARD_PAYMENT_WINDOW_DAYS = 45;
    private static final int REFUND_WINDOW_DAYS = 90;
    private static final int CASHBACK_WINDOW_DAYS = 30;

    private final TransactionRepository transactionRepository;
    private final TransactionLinkRepository transactionLinkRepository;
    private final ReconciliationReviewRepository reviewRepository;

    /**
     * Runs reconciliation for a batch of newly imported transactions.
     */
    public void reconcile(List<Transaction> newTransactions, UUID userId) {
        for (Transaction tx : newTransactions) {
            if (tx.getTransactionType() == TransactionType.CREDIT_CARD_PAYMENT) {
                detectCardPaymentMatch(tx, userId);
            } else if (tx.getTransactionType() == TransactionType.REFUND) {
                detectRefundMatch(tx);
            } else if (tx.getTransactionType() == TransactionType.CASHBACK) {
                detectCashbackMatch(tx);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Credit card payment detection
    // -------------------------------------------------------------------------

    /**
     * A bank DEBIT classified as CREDIT_CARD_PAYMENT may correspond to a card statement.
     * Looks for card transactions with the same amount within the payment window.
     */
    private void detectCardPaymentMatch(Transaction bankPayment, UUID userId) {
        if (bankPayment.getAccount() == null) return; // must be a bank transaction

        LocalDate from = bankPayment.getTransactionDate().minusDays(CARD_PAYMENT_WINDOW_DAYS);
        LocalDate to = bankPayment.getTransactionDate().plusDays(5); // small forward window

        List<Transaction> allUserTx = transactionRepository.findByUserAndDateRange(userId, from, to);

        for (Transaction candidate : allUserTx) {
            if (candidate.getId().equals(bankPayment.getId())) continue;
            if (candidate.getCard() == null) continue; // must be a card transaction
            if (linkExists(bankPayment, candidate, TransactionLinkType.CREDIT_CARD_PAYMENT)) continue;

            if (bankPayment.getAmount().compareTo(candidate.getAmount()) == 0) {
                BigDecimal confidence = computeCardPaymentConfidence(bankPayment, candidate);
                createLink(bankPayment, candidate,
                        TransactionLinkType.CREDIT_CARD_PAYMENT,
                        ReviewType.POSSIBLE_CARD_PAYMENT,
                        confidence,
                        "Bank debit of " + bankPayment.getAmount() + " matches card transaction of same amount within " + CARD_PAYMENT_WINDOW_DAYS + " days.");
            }
        }
    }

    private BigDecimal computeCardPaymentConfidence(Transaction bankTx, Transaction cardTx) {
        double score = 0.75;
        // Higher confidence if bank description contains card-related keywords
        String desc = bankTx.getDescription();
        if (desc.contains("CREDIT CARD") || desc.contains("CARD PAYMENT") || desc.contains("CARD BILL")) {
            score += 0.15;
        }
        // Closer in time = higher confidence
        long days = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                bankTx.getTransactionDate(), cardTx.getTransactionDate()));
        if (days <= 7) score += 0.05;
        return BigDecimal.valueOf(Math.min(score, 0.95));
    }

    // -------------------------------------------------------------------------
    // Refund detection
    // -------------------------------------------------------------------------

    /**
     * A CREDIT classified as REFUND may correspond to a prior DEBIT on the same source.
     */
    private void detectRefundMatch(Transaction refund) {
        List<Transaction> candidates = getSameSourceTransactions(refund);

        for (Transaction candidate : candidates) {
            if (candidate.getId().equals(refund.getId())) continue;
            if (candidate.getDirection() != TransactionDirection.DEBIT) continue;
            if (linkExists(refund, candidate, TransactionLinkType.REFUND)) continue;

            if (refund.getAmount().compareTo(candidate.getAmount()) == 0) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                        candidate.getTransactionDate(), refund.getTransactionDate());
                if (days >= 0 && days <= REFUND_WINDOW_DAYS) {
                    double score = 0.70;
                    if (descriptionSimilar(refund.getDescription(), candidate.getDescription())) score += 0.15;
                    if (days <= 14) score += 0.10;
                    createLink(refund, candidate,
                            TransactionLinkType.REFUND,
                            ReviewType.POSSIBLE_REFUND,
                            BigDecimal.valueOf(score),
                            "Credit of " + refund.getAmount() + " may be a refund for debit on " + candidate.getTransactionDate() + ".");
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cashback detection
    // -------------------------------------------------------------------------

    /**
     * A CREDIT classified as CASHBACK on a card may be linked to a prior purchase.
     * Cashback is typically a small percentage of the purchase amount.
     */
    private void detectCashbackMatch(Transaction cashback) {
        if (cashback.getCard() == null) return; // cashback is card-specific

        List<Transaction> candidates = getSameSourceTransactions(cashback);

        for (Transaction candidate : candidates) {
            if (candidate.getId().equals(cashback.getId())) continue;
            if (candidate.getDirection() != TransactionDirection.DEBIT) continue;
            if (candidate.getTransactionType() == TransactionType.CREDIT_CARD_PAYMENT) continue;
            if (linkExists(cashback, candidate, TransactionLinkType.CASHBACK)) continue;

            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    candidate.getTransactionDate(), cashback.getTransactionDate());
            if (days < 0 || days > CASHBACK_WINDOW_DAYS) continue;

            // Cashback is typically 0.5%–5% of purchase
            BigDecimal maxCashback = candidate.getAmount().multiply(BigDecimal.valueOf(0.10));
            if (cashback.getAmount().compareTo(maxCashback) <= 0) {
                createLink(cashback, candidate,
                        TransactionLinkType.CASHBACK,
                        ReviewType.POSSIBLE_CASHBACK,
                        BigDecimal.valueOf(0.70),
                        "Cashback of " + cashback.getAmount() + " may relate to purchase of " + candidate.getAmount() + " on " + candidate.getTransactionDate() + ".");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void createLink(Transaction source, Transaction target,
                             TransactionLinkType linkType, ReviewType reviewType,
                             BigDecimal confidence, String reason) {
        TransactionLink link = new TransactionLink();
        link.setSourceTransaction(source);
        link.setTargetTransaction(target);
        link.setLinkType(linkType);
        link.setConfidenceScore(confidence);
        link.setStatus(TransactionLinkStatus.REVIEW_REQUIRED);
        link.setReason(reason);
        link = transactionLinkRepository.save(link);

        source.setReconciliationStatus(ReconciliationStatus.REVIEW_REQUIRED);
        transactionRepository.save(source);

        ReconciliationReview review = new ReconciliationReview();
        review.setTransactionLink(link);
        review.setReviewType(reviewType);
        review.setStatus(ReviewStatus.PENDING);
        review.setSystemReason(reason);
        reviewRepository.save(review);

        log.info("Reconciliation review created [{}]: {} ↔ {} (confidence {})",
                linkType, source.getId(), target.getId(), confidence);
    }

    private List<Transaction> getSameSourceTransactions(Transaction tx) {
        if (tx.getAccount() != null) {
            return transactionRepository.findByAccountIdOrderByTransactionDateDesc(
                    tx.getAccount().getId());
        } else {
            return transactionRepository.findByCardIdOrderByTransactionDateDesc(
                    tx.getCard().getId());
        }
    }

    private boolean linkExists(Transaction a, Transaction b, TransactionLinkType type) {
        return transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(
                a.getId(), b.getId(), type)
                || transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(
                b.getId(), a.getId(), type);
    }

    private boolean descriptionSimilar(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        int len = Math.min(10, Math.min(a.length(), b.length()));
        return len > 0 && (a.contains(b.substring(0, len)) || b.contains(a.substring(0, len)));
    }
}
