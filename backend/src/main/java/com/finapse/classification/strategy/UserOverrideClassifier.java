package com.finapse.classification.strategy;

import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.entity.UserClassificationRule;
import com.finapse.enums.ClassificationSource;
import com.finapse.repository.UserClassificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class UserOverrideClassifier implements ClassificationStrategy {

    private final UserClassificationRuleRepository ruleRepository;

    @Override
    public boolean supports(ClassificationContext context) {
        return context.getNormalizedNarration() != null
                && context.getTransaction() != null
                && getUserId(context) != null;
    }

    @Override
    @Transactional
    public ClassificationResult classify(ClassificationContext context) {
        UUID userId = getUserId(context);
        if (userId == null) return null;

        String narration = context.getNormalizedNarration();
        List<UserClassificationRule> rules = ruleRepository
                .findByUserIdAndIsActiveTrueOrderByTimesAppliedDesc(userId);

        for (UserClassificationRule rule : rules) {
            if (matches(narration, rule)) {
                ruleRepository.incrementTimesApplied(rule.getId());
                log.debug("User rule matched: '{}' → {} (applied {} times)",
                        rule.getNarrationPattern(), rule.getTransactionType(), rule.getTimesApplied() + 1);

                var builder = ClassificationResult.builder()
                        .transactionType(rule.getTransactionType())
                        .confidence(1.0)
                        .source(ClassificationSource.USER_OVERRIDE)
                        .reason("User classification rule: " + rule.getNarrationPattern())
                        .needsReview(false);

                if (rule.getCategory() != null) {
                    builder.category(rule.getCategory());
                }
                if (rule.getMerchant() != null) {
                    builder.merchant(rule.getMerchant());
                }

                return builder.build();
            }
        }

        return null;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private boolean matches(String narration, UserClassificationRule rule) {
        String pattern = rule.getNarrationPattern();
        return switch (rule.getMatchType()) {
            case EXACT -> narration.equals(pattern);
            case CONTAINS -> narration.contains(pattern);
            case STARTS_WITH -> narration.startsWith(pattern);
        };
    }

    private UUID getUserId(ClassificationContext context) {
        var tx = context.getTransaction();
        if (tx == null) return null;
        if (tx.getAccount() != null && tx.getAccount().getUser() != null) {
            return tx.getAccount().getUser().getId();
        }
        if (tx.getCard() != null && tx.getCard().getUser() != null) {
            return tx.getCard().getUser().getId();
        }
        if (tx.getStatement() != null && tx.getStatement().getUser() != null) {
            return tx.getStatement().getUser().getId();
        }
        return null;
    }
}
