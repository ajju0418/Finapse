package com.finapse.service;

import com.finapse.entity.*;
import com.finapse.enums.TransactionType;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.UserClassificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserClassificationRuleService {

    private final UserClassificationRuleRepository ruleRepository;

    @Transactional
    public UserClassificationRule createRuleFromCorrection(
            User user,
            String normalizedNarration,
            TransactionType correctedType,
            Category category,
            Merchant merchant) {

        List<UserClassificationRule> existing = ruleRepository
                .findExactRulesByUserAndPattern(user.getId(), normalizedNarration);

        if (!existing.isEmpty()) {
            UserClassificationRule rule = existing.get(0);
            rule.setTransactionType(correctedType);
            rule.setCategory(category);
            rule.setMerchant(merchant);
            log.info("Updated existing user rule for pattern: '{}'", normalizedNarration);
            return ruleRepository.save(rule);
        }

        UserClassificationRule rule = new UserClassificationRule();
        rule.setUser(user);
        rule.setNarrationPattern(normalizedNarration);
        rule.setMatchType(UserClassificationRule.MatchType.CONTAINS);
        rule.setTransactionType(correctedType);
        rule.setCategory(category);
        rule.setMerchant(merchant);

        log.info("Created new user rule: '{}' → {}", normalizedNarration, correctedType);
        return ruleRepository.save(rule);
    }

    public List<UserClassificationRule> getUserRules(UUID userId) {
        return ruleRepository.findByUserIdAndIsActiveTrueOrderByTimesAppliedDesc(userId);
    }

    @Transactional
    public void deactivateRule(UUID ruleId) {
        ruleRepository.findById(ruleId).ifPresent(rule -> {
            rule.setActive(false);
            ruleRepository.save(rule);
        });
    }

    /**
     * Deactivates a rule only if it belongs to the given user, so one account
     * cannot delete another's learned rules by guessing an id.
     */
    @Transactional
    public void deactivateRuleForUser(UUID ruleId, UUID userId) {
        ruleRepository.findById(ruleId)
                .filter(rule -> rule.getUser() != null && rule.getUser().getId().equals(userId))
                .ifPresentOrElse(rule -> {
                    rule.setActive(false);
                    ruleRepository.save(rule);
                    log.info("Deactivated rule {} for user {}", ruleId, userId);
                }, () -> {
                    throw new ResourceNotFoundException("Rule not found: " + ruleId);
                });
    }
}
