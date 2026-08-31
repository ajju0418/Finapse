package com.finapse.service;

import com.finapse.classification.detection.NormalizationService;
import com.finapse.dto.LearnedRuleResponse;
import com.finapse.dto.TransactionCorrectionRequest;
import com.finapse.dto.TransactionResponse;
import com.finapse.entity.Category;
import com.finapse.entity.Transaction;
import com.finapse.entity.User;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.TransactionType;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final UserService userService;
    private final UserClassificationRuleService ruleService;
    private final NormalizationService normalizationService;

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByStatement(UUID statementId) {
        return transactionRepository.findByStatementIdOrderByTransactionDateDesc(statementId)
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByCard(UUID cardId) {
        return transactionRepository.findByCardIdOrderByTransactionDateDesc(cardId)
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getByAccount(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId)
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        return TransactionResponse.from(findOrThrow(id));
    }

    /**
     * Records a user's correction and, unless they opt out, teaches the classifier
     * from it by persisting a {@code UserClassificationRule}.
     *
     * <p>The rule is keyed on the <em>normalized</em> narration — the same form the
     * classifier sees at import time — so it generalises across the reference numbers
     * and city names that make raw descriptions unique.
     */
    @Transactional
    public TransactionResponse applyCorrection(UUID id, TransactionCorrectionRequest request) {
        Transaction tx = findOrThrow(id);
        User user = userService.getCurrentUser();

        TransactionType previousType = tx.getTransactionType();
        tx.setTransactionType(request.transactionType());

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryService.findOrThrow(request.categoryId());
            tx.setCategory(category);
        }

        // The user's judgement outranks anything the engine inferred.
        tx.setClassificationSource(ClassificationSource.USER_OVERRIDE);
        tx.setClassificationConfidence(1.0);
        tx.setClassificationReason("Corrected by you"
                + (previousType != request.transactionType()
                        ? " (was " + previousType + ")"
                        : ""));

        Transaction saved = transactionRepository.save(tx);

        if (request.shouldLearn()) {
            String pattern = normalizationService.normalize(tx.getDescription());
            if (pattern != null && !pattern.isBlank()) {
                ruleService.createRuleFromCorrection(
                        user,
                        pattern,
                        request.transactionType(),
                        category != null ? category : tx.getCategory(),
                        tx.getMerchant());
                log.info("Learned rule from correction on transaction {}: '{}' -> {}",
                        id, pattern, request.transactionType());
            }
        }

        return TransactionResponse.from(saved);
    }

    /** Rules the engine has learned from this user's corrections. */
    @Transactional(readOnly = true)
    public List<LearnedRuleResponse> getLearnedRules() {
        return ruleService.getUserRules(userService.getCurrentUserId())
                .stream().map(LearnedRuleResponse::from).toList();
    }

    /** Forgets a learned rule so future imports stop applying it. */
    @Transactional
    public void forgetRule(UUID ruleId) {
        ruleService.deactivateRuleForUser(ruleId, userService.getCurrentUserId());
    }

    @Transactional
    public TransactionResponse updateType(UUID id, TransactionType type) {
        // Kept for the existing query-param endpoint; does not create a rule.
        return applyCorrection(id, new TransactionCorrectionRequest(type, null, false));
    }

    @Transactional
    public TransactionResponse updateCategory(UUID id, UUID categoryId) {
        Transaction tx = findOrThrow(id);
        tx.setCategory(categoryService.findOrThrow(categoryId));
        return TransactionResponse.from(transactionRepository.save(tx));
    }

    /**
     * Loads a transaction belonging to the signed-in user.
     *
     * <p>Scoped by user id so a transaction cannot be read or modified by guessing
     * its identifier.
     */
    public Transaction findOrThrow(UUID id) {
        return transactionRepository.findByIdAndUserId(id, userService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }
}
