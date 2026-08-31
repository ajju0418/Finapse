package com.finapse.classification.strategy;

import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.entity.Transaction;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.TransactionType;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class HistoricalPatternClassifier implements ClassificationStrategy {

    private static final int MIN_HISTORY_COUNT = 3;
    private static final double MIN_MAJORITY_RATIO = 0.6;

    private final TransactionRepository transactionRepository;

    @Override
    public boolean supports(ClassificationContext context) {
        return context.getNormalizedNarration() != null
                && context.getTransaction() != null
                && getUserId(context) != null;
    }

    @Override
    public ClassificationResult classify(ClassificationContext context) {
        UUID userId = getUserId(context);
        if (userId == null) return null;

        ClassificationResult merchantResult = classifyByMerchantHistory(context, userId);
        if (merchantResult != null) return merchantResult;

        return classifyByNarrationHistory(context, userId);
    }

    @Override
    public int getPriority() {
        return 3;
    }

    private ClassificationResult classifyByMerchantHistory(ClassificationContext context, UUID userId) {
        if (context.getMerchant() == null) return null;

        List<Transaction> history = transactionRepository
                .findByUserAndMerchant(userId, context.getMerchant().getId());

        return buildResultFromHistory(history, "Merchant history: " + context.getMerchant().getName());
    }

    private ClassificationResult classifyByNarrationHistory(ClassificationContext context, UUID userId) {
        String narration = context.getNormalizedNarration();
        String searchPattern = extractSearchPattern(narration);
        if (searchPattern == null || searchPattern.length() < 4) return null;

        List<Transaction> history = transactionRepository
                .findHighConfidenceByUserAndPattern(userId, searchPattern);

        return buildResultFromHistory(history, "Narration history pattern: " + searchPattern);
    }

    private ClassificationResult buildResultFromHistory(List<Transaction> history, String reason) {
        if (history.size() < MIN_HISTORY_COUNT) return null;

        Map<TransactionType, Long> typeCounts = history.stream()
                .filter(t -> t.getTransactionType() != TransactionType.UNKNOWN)
                .collect(Collectors.groupingBy(Transaction::getTransactionType, Collectors.counting()));

        if (typeCounts.isEmpty()) return null;

        var mostCommon = typeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (mostCommon == null) return null;

        long total = typeCounts.values().stream().mapToLong(Long::longValue).sum();
        double ratio = (double) mostCommon.getValue() / total;

        if (ratio < MIN_MAJORITY_RATIO) return null;

        double confidence = 0.60 + (ratio * 0.25) + (Math.min(history.size(), 10) * 0.01);
        confidence = Math.min(confidence, 0.92);

        var builder = ClassificationResult.builder()
                .transactionType(mostCommon.getKey())
                .confidence(confidence)
                .source(ClassificationSource.HISTORICAL)
                .reason(reason + " (" + mostCommon.getValue() + "/" + total + " = " + String.format("%.0f%%", ratio * 100) + ")")
                .needsReview(confidence < 0.75);

        var categoryVote = history.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(t -> t.getCategory().getId(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (categoryVote != null) {
            history.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryVote.getKey()))
                    .findFirst()
                    .ifPresent(t -> builder.category(t.getCategory()));
        }

        return builder.build();
    }

    private String extractSearchPattern(String narration) {
        if (narration == null || narration.isBlank()) return null;
        String[] tokens = narration.split("\\s+");
        if (tokens.length == 0) return null;
        if (tokens.length == 1) return tokens[0];
        return tokens[0] + " " + tokens[1];
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
