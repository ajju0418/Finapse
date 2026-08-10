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
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Detects potential duplicate transactions.
 *
 * Two detection strategies:
 * 1. Exact hash match — same fingerprint (source + date + amount + direction + description).
 * 2. Near-duplicate — same amount, direction, and source within 3 days with similar description.
 *
 * Never deletes transactions. Creates TransactionLink + ReconciliationReview for user review.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final TransactionRepository transactionRepository;
    private final TransactionLinkRepository transactionLinkRepository;
    private final ReconciliationReviewRepository reviewRepository;

    /**
     * Runs duplicate detection for a batch of newly imported transactions.
     * Checks each transaction against all existing transactions (excluding itself).
     */
    public void detectDuplicates(List<Transaction> newTransactions) {
        for (Transaction tx : newTransactions) {
            checkForDuplicates(tx);
        }
    }

    private void checkForDuplicates(Transaction tx) {
        // 1. Exact hash match
        if (tx.getTransactionHash() != null) {
            List<Transaction> hashMatches = transactionRepository
                    .findByTransactionHashAndIdNot(tx.getTransactionHash(), tx.getId());
            for (Transaction match : hashMatches) {
                if (!linkExists(tx, match, TransactionLinkType.DUPLICATE)) {
                    createDuplicateReview(tx, match, new BigDecimal("0.95"),
                            "Exact transaction fingerprint match (same source, date, amount, direction, description).");
                }
            }
        }

        // 2. Near-duplicate: same source, same amount, same direction, within 3 days
        List<Transaction> candidates = getSameSourceTransactions(tx);
        for (Transaction candidate : candidates) {
            if (candidate.getId().equals(tx.getId())) continue;
            if (linkExists(tx, candidate, TransactionLinkType.DUPLICATE)) continue;

            if (isNearDuplicate(tx, candidate)) {
                BigDecimal confidence = computeNearDuplicateConfidence(tx, candidate);
                createDuplicateReview(tx, candidate, confidence,
                        "Near-duplicate: same amount, direction, and source within 3 days.");
            }
        }
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

    private boolean isNearDuplicate(Transaction a, Transaction b) {
        if (!a.getAmount().equals(b.getAmount())) return false;
        if (a.getDirection() != b.getDirection()) return false;
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(a.getTransactionDate(), b.getTransactionDate()));
        return daysDiff <= 3;
    }

    private BigDecimal computeNearDuplicateConfidence(Transaction a, Transaction b) {
        // Base: 0.70 for amount+direction+source match within 3 days
        // +0.15 if same day
        // +0.10 if descriptions are similar
        double score = 0.70;
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(a.getTransactionDate(), b.getTransactionDate()));
        if (daysDiff == 0) score += 0.15;
        if (descriptionSimilar(a.getDescription(), b.getDescription())) score += 0.10;
        return BigDecimal.valueOf(Math.min(score, 0.94)); // cap below exact-hash confidence
    }

    private boolean descriptionSimilar(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        // Check if one contains the other (handles truncated descriptions)
        return a.contains(b.substring(0, Math.min(10, b.length())))
                || b.contains(a.substring(0, Math.min(10, a.length())));
    }

    private void createDuplicateReview(Transaction source, Transaction target,
                                       BigDecimal confidence, String reason) {
        TransactionLink link = new TransactionLink();
        link.setSourceTransaction(source);
        link.setTargetTransaction(target);
        link.setLinkType(TransactionLinkType.DUPLICATE);
        link.setConfidenceScore(confidence);
        link.setStatus(TransactionLinkStatus.REVIEW_REQUIRED);
        link.setReason(reason);
        link = transactionLinkRepository.save(link);

        source.setReconciliationStatus(ReconciliationStatus.REVIEW_REQUIRED);
        transactionRepository.save(source);

        ReconciliationReview review = new ReconciliationReview();
        review.setTransactionLink(link);
        review.setReviewType(ReviewType.POSSIBLE_DUPLICATE);
        review.setStatus(ReviewStatus.PENDING);
        review.setSystemReason(reason);
        reviewRepository.save(review);

        log.info("Duplicate review created: {} ↔ {} (confidence {})", source.getId(), target.getId(), confidence);
    }

    private boolean linkExists(Transaction a, Transaction b, TransactionLinkType type) {
        return transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(
                a.getId(), b.getId(), type)
                || transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(
                b.getId(), a.getId(), type);
    }
}
