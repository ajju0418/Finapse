package com.finapse.dto;

import com.finapse.entity.ReconciliationReview;
import com.finapse.enums.ReviewStatus;
import com.finapse.enums.ReviewType;
import com.finapse.enums.TransactionLinkType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReconciliationReviewResponse(
        UUID id,
        UUID linkId,
        TransactionLinkType linkType,
        BigDecimal confidenceScore,
        ReviewType reviewType,
        ReviewStatus status,
        String systemReason,
        String userDecision,
        UUID sourceTransactionId,
        UUID targetTransactionId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public static ReconciliationReviewResponse from(ReconciliationReview review) {
        var link = review.getTransactionLink();
        return new ReconciliationReviewResponse(
                review.getId(),
                link.getId(),
                link.getLinkType(),
                link.getConfidenceScore(),
                review.getReviewType(),
                review.getStatus(),
                review.getSystemReason(),
                review.getUserDecision(),
                link.getSourceTransaction().getId(),
                link.getTargetTransaction().getId(),
                review.getCreatedAt(),
                review.getReviewedAt()
        );
    }
}
