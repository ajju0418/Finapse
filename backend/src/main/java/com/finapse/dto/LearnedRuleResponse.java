package com.finapse.dto;

import com.finapse.entity.UserClassificationRule;
import com.finapse.enums.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

/** A rule the engine learned from a user correction. */
public record LearnedRuleResponse(
        UUID id,
        String narrationPattern,
        String matchType,
        TransactionType transactionType,
        String categoryName,
        String merchantName,
        int timesApplied,
        LocalDateTime createdAt
) {
    public static LearnedRuleResponse from(UserClassificationRule rule) {
        return new LearnedRuleResponse(
                rule.getId(),
                rule.getNarrationPattern(),
                rule.getMatchType().name(),
                rule.getTransactionType(),
                rule.getCategory() != null ? rule.getCategory().getDisplayName() : null,
                rule.getMerchant() != null ? rule.getMerchant().getName() : null,
                rule.getTimesApplied(),
                rule.getCreatedAt());
    }
}
