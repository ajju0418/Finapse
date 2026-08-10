package com.finapse.service;

import com.finapse.dto.ReconciliationDecisionRequest;
import com.finapse.dto.ReconciliationReviewResponse;
import com.finapse.entity.ReconciliationReview;
import com.finapse.entity.Transaction;
import com.finapse.entity.TransactionLink;
import com.finapse.enums.*;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.ReconciliationReviewRepository;
import com.finapse.repository.TransactionLinkRepository;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationReviewService {

    private final ReconciliationReviewRepository reviewRepository;
    private final TransactionLinkRepository linkRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<ReconciliationReviewResponse> getPending() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING)
                .stream().map(ReconciliationReviewResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reviewRepository.countByStatus(ReviewStatus.PENDING);
    }

    /**
     * Applies the user's decision (APPROVED or REJECTED) to a reconciliation review.
     *
     * On APPROVED:
     *  - Link status → CONFIRMED
     *  - Both transactions → reconciliation status updated
     *  - For CREDIT_CARD_PAYMENT: bank tx type → CREDIT_CARD_PAYMENT
     *  - For DUPLICATE: source tx type → UNKNOWN (it's the duplicate)
     *
     * On REJECTED:
     *  - Link status → REJECTED
     *  - Both transactions → UNMATCHED
     */
    @Transactional
    public ReconciliationReviewResponse decide(UUID reviewId, ReconciliationDecisionRequest request) {
        ReconciliationReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("Review " + reviewId + " is already " + review.getStatus());
        }

        TransactionLink link = review.getTransactionLink();
        Transaction source = link.getSourceTransaction();
        Transaction target = link.getTargetTransaction();

        if (request.approved()) {
            applyApproval(link, source, target);
            review.setStatus(ReviewStatus.APPROVED);
        } else {
            link.setStatus(TransactionLinkStatus.REJECTED);
            source.setReconciliationStatus(ReconciliationStatus.UNMATCHED);
            review.setStatus(ReviewStatus.REJECTED);
        }

        review.setUserDecision(request.note());
        review.setReviewedAt(LocalDateTime.now());
        link.setReviewedAt(LocalDateTime.now());

        linkRepository.save(link);
        transactionRepository.save(source);
        reviewRepository.save(review);

        log.info("Review {} decided: {}", reviewId, review.getStatus());
        return ReconciliationReviewResponse.from(review);
    }

    private void applyApproval(TransactionLink link, Transaction source, Transaction target) {
        link.setStatus(TransactionLinkStatus.CONFIRMED);

        switch (link.getLinkType()) {
            case CREDIT_CARD_PAYMENT -> {
                source.setTransactionType(TransactionType.CREDIT_CARD_PAYMENT);
                source.setReconciliationStatus(ReconciliationStatus.CONFIRMED_CARD_PAYMENT);
                target.setReconciliationStatus(ReconciliationStatus.CONFIRMED_CARD_PAYMENT);
                transactionRepository.save(target);
            }
            case DUPLICATE -> {
                source.setReconciliationStatus(ReconciliationStatus.CONFIRMED_DUPLICATE);
                target.setReconciliationStatus(ReconciliationStatus.CONFIRMED_DUPLICATE);
                transactionRepository.save(target);
            }
            case TRANSFER -> {
                source.setTransactionType(TransactionType.TRANSFER);
                source.setReconciliationStatus(ReconciliationStatus.CONFIRMED_TRANSFER);
                target.setReconciliationStatus(ReconciliationStatus.CONFIRMED_TRANSFER);
                transactionRepository.save(target);
            }
            case REFUND -> {
                source.setTransactionType(TransactionType.REFUND);
                source.setReconciliationStatus(ReconciliationStatus.MATCHED);
                target.setReconciliationStatus(ReconciliationStatus.MATCHED);
                transactionRepository.save(target);
            }
            case CASHBACK -> {
                source.setTransactionType(TransactionType.CASHBACK);
                source.setReconciliationStatus(ReconciliationStatus.MATCHED);
                target.setReconciliationStatus(ReconciliationStatus.MATCHED);
                transactionRepository.save(target);
            }
        }
    }
}
