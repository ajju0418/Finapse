package com.finapse.classification.orchestrator;

import com.finapse.classification.detection.MerchantDetectionService;
import com.finapse.classification.detection.MerchantMatchResult;
import com.finapse.classification.detection.NormalizationService;
import com.finapse.classification.detection.RecurringTransactionDetector;
import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.classification.strategy.ClassificationStrategy;
import com.finapse.dto.RawTransactionRecord;
import com.finapse.entity.Account;
import com.finapse.entity.Card;
import com.finapse.entity.Merchant;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.ReconciliationStatus;
import com.finapse.enums.TransactionType;
import com.finapse.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificationOrchestrator {

    private final NormalizationService normalizationService;
    private final MerchantDetectionService merchantDetectionService;
    private final RecurringTransactionDetector recurringTransactionDetector;
    private final List<ClassificationStrategy> strategies;

    public Transaction orchestrate(RawTransactionRecord raw, Statement statement, Account account, Card card) {
        Transaction tx = new Transaction();
        tx.setStatement(statement);
        tx.setAccount(account);
        tx.setCard(card);
        tx.setTransactionDate(raw.transactionDate());
        tx.setPostedDate(raw.postedDate());
        tx.setDescription(raw.description());
        tx.setAmount(raw.amount());
        tx.setDirection(raw.direction());
        tx.setReconciliationStatus(ReconciliationStatus.UNMATCHED);
        tx.setSourceRowNumber(raw.sourceRowNumber());

        String normalizedNarration = normalizationService.normalize(raw.description());
        var merchantMatch = merchantDetectionService.detectWithConfidence(normalizedNarration);

        tx.setTransactionHash(buildHash(raw, account, card, normalizedNarration));
        if (merchantMatch.isPresent()) {
            tx.setMerchant(merchantMatch.merchant());
        }

        Optional<Merchant> merchantOpt = merchantMatch.isPresent()
                ? Optional.of(merchantMatch.merchant())
                : Optional.empty();

        tx = processClassification(tx, raw.description(), normalizedNarration, merchantOpt);
        tx = applyRecurringDetection(tx, statement);

        return tx;
    }

    public Transaction reclassify(Transaction tx) {
        String normalizedNarration = normalizationService.normalize(tx.getDescription());
        var merchantMatch = merchantDetectionService.detectWithConfidence(normalizedNarration);
        if (merchantMatch.isPresent()) {
            tx.setMerchant(merchantMatch.merchant());
        }

        Optional<Merchant> merchantOpt = merchantMatch.isPresent()
                ? Optional.of(merchantMatch.merchant())
                : Optional.empty();

        return processClassification(tx, tx.getDescription(), normalizedNarration, merchantOpt);
    }

    private Transaction processClassification(Transaction tx, String rawDesc, String normalizedDesc, Optional<Merchant> merchantOpt) {
        ClassificationContext context = ClassificationContext.builder()
                .transaction(tx)
                .rawNarration(rawDesc)
                .normalizedNarration(normalizedDesc)
                .amount(tx.getAmount())
                .direction(tx.getDirection())
                .merchant(merchantOpt.orElse(null))
                .build();

        List<ClassificationResult> allResults = new ArrayList<>();

        for (ClassificationStrategy strategy : strategies) {
            if (strategy.supports(context)) {
                ClassificationResult result = strategy.classify(context);
                if (result != null) {
                    allResults.add(result);

                    if (result.getConfidence() >= 0.95
                            && result.getSource() == ClassificationSource.USER_OVERRIDE) {
                        applyResult(tx, result);
                        return tx;
                    }
                }
            }
        }

        ClassificationResult finalResult = blendResults(allResults);

        if (finalResult != null) {
            applyResult(tx, finalResult);
        } else {
            tx.setTransactionType(TransactionType.UNKNOWN);
            tx.setClassificationSource(ClassificationSource.UNKNOWN);
            tx.setClassificationConfidence(0.0);
            tx.setClassificationReason("No strategy could confidently classify this transaction");
        }

        return tx;
    }

    /** The reason column is capped at 500 chars; blended reasons can run long. */
    private String truncateReason(String reason) {
        if (reason == null) return null;
        return reason.length() <= 500 ? reason : reason.substring(0, 497) + "...";
    }

    private ClassificationResult blendResults(List<ClassificationResult> results) {
        if (results.isEmpty()) return null;
        if (results.size() == 1) return results.get(0);

        Map<TransactionType, Double> typeScores = new HashMap<>();
        Map<TransactionType, ClassificationResult> bestResultForType = new HashMap<>();

        for (ClassificationResult result : results) {
            TransactionType type = result.getTransactionType();
            if (type == null || type == TransactionType.UNKNOWN) continue;

            double weight = computeSourceWeight(result.getSource());
            double weightedScore = result.getConfidence() * weight;

            typeScores.merge(type, weightedScore, Double::sum);

            ClassificationResult current = bestResultForType.get(type);
            if (current == null || result.getConfidence() > current.getConfidence()) {
                bestResultForType.put(type, result);
            }
        }

        if (typeScores.isEmpty()) return results.get(0);

        var winner = typeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (winner == null) return results.get(0);

        ClassificationResult bestResult = bestResultForType.get(winner.getKey());

        double totalScore = typeScores.values().stream().mapToDouble(Double::doubleValue).sum();
        double winnerRatio = winner.getValue() / totalScore;
        long agreeing = results.stream()
                .filter(r -> r.getTransactionType() == winner.getKey())
                .count();

        double blendedConfidence = bestResult.getConfidence();
        if (agreeing > 1) {
            double consensusBoost = Math.min(agreeing * 0.03, 0.10);
            blendedConfidence = Math.min(blendedConfidence + consensusBoost, 0.99);
        }
        if (winnerRatio < 0.5) {
            blendedConfidence *= 0.85;
        }

        return ClassificationResult.builder()
                .transactionType(winner.getKey())
                .category(bestResult.getCategory())
                .merchant(bestResult.getMerchant())
                .confidence(blendedConfidence)
                .source(bestResult.getSource())
                .reason(buildBlendedReason(results, winner.getKey(), agreeing))
                .needsReview(blendedConfidence < 0.75)
                .build();
    }

    private double computeSourceWeight(ClassificationSource source) {
        if (source == null) return 0.5;
        return switch (source) {
            case USER_OVERRIDE -> 2.0;
            case MERCHANT_DATABASE -> 1.5;
            case HISTORICAL -> 1.3;
            case EXACT_RULE -> 1.0;
            case PATTERN -> 0.9;
            case FUZZY_RULE -> 0.7;
            case LLM -> 0.8;
            case UNKNOWN -> 0.5;
        };
    }

    private String buildBlendedReason(List<ClassificationResult> results, TransactionType winner, long agreeing) {
        long total = results.stream()
                .filter(r -> r.getTransactionType() != null && r.getTransactionType() != TransactionType.UNKNOWN)
                .count();

        StringBuilder sb = new StringBuilder();
        sb.append("Blended: ").append(agreeing).append("/").append(total).append(" strategies agree on ").append(winner);

        results.stream()
                .filter(r -> r.getTransactionType() == winner && r.getReason() != null)
                .findFirst()
                .ifPresent(r -> sb.append(" [").append(r.getReason()).append("]"));

        return sb.toString();
    }

    private void applyResult(Transaction tx, ClassificationResult result) {
        tx.setTransactionType(result.getTransactionType() != null ? result.getTransactionType() : TransactionType.UNKNOWN);
        // Source and reason are what the UI shows to explain the decision, so never
        // leave them null — an unattributed classification is indistinguishable from
        // one that was never attempted.
        tx.setClassificationSource(result.getSource() != null ? result.getSource() : ClassificationSource.UNKNOWN);
        tx.setClassificationConfidence(result.getConfidence());
        tx.setClassificationReason(truncateReason(
                result.getReason() != null ? result.getReason() : "Classified as " + tx.getTransactionType()));

        if (result.getMerchant() != null) {
            tx.setMerchant(result.getMerchant());
        }
        if (result.getCategory() != null) {
            tx.setCategory(result.getCategory());
        }

        if (result.isNeedsReview()) {
            log.info("Transaction needs review: {} (confidence: {}, source: {})",
                    tx.getDescription(), result.getConfidence(), result.getSource());
        }
    }

    private Transaction applyRecurringDetection(Transaction tx, Statement statement) {
        UUID userId = getUserId(tx, statement);
        if (userId == null || tx.getMerchant() == null) return tx;

        var recurringResult = recurringTransactionDetector.checkIfRecurring(tx, userId);
        if (recurringResult.isRecurring()) {
            tx.setIsRecurring(true);
            tx.setRecurringGroupId(recurringResult.groupId());

            if (tx.getTransactionType() == TransactionType.EXPENSE
                    || tx.getTransactionType() == TransactionType.UNKNOWN) {
                if (recurringResult.suggestedType() != null
                        && recurringResult.confidence() > 0.70) {
                    tx.setTransactionType(recurringResult.suggestedType());
                    log.info("Recurring detection upgraded type to {} for: {}",
                            recurringResult.suggestedType(), tx.getDescription());
                }
            }
        }
        return tx;
    }

    private UUID getUserId(Transaction tx, Statement statement) {
        if (statement != null && statement.getUser() != null) {
            return statement.getUser().getId();
        }
        if (tx.getAccount() != null && tx.getAccount().getUser() != null) {
            return tx.getAccount().getUser().getId();
        }
        if (tx.getCard() != null && tx.getCard().getUser() != null) {
            return tx.getCard().getUser().getId();
        }
        return null;
    }

    private String buildHash(RawTransactionRecord raw, Account account, Card card, String normalizedDesc) {
        String sourceId = account != null ? account.getId().toString() : card.getId().toString();
        String input = sourceId
                + "|" + raw.transactionDate()
                + "|" + raw.amount().toPlainString()
                + "|" + raw.direction().name()
                + "|" + normalizedDesc;
        return HashUtil.sha256(input);
    }
}
