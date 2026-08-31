package com.finapse.classification.strategy;

import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.entity.Category;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import com.finapse.service.CategoryInferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
@RequiredArgsConstructor
public class CategoryKeywordClassifier implements ClassificationStrategy {

    private final CategoryInferenceService categoryInferenceService;

    @Override
    public boolean supports(ClassificationContext context) {
        return context.getNormalizedNarration() != null
                && !context.getNormalizedNarration().isBlank();
    }

    @Override
    public ClassificationResult classify(ClassificationContext context) {
        Category category = categoryInferenceService.infer(context.getNormalizedNarration());
        if (category == null) {
            return null;
        }

        TransactionType type = inferTypeFromCategory(category.getName(), context.getDirection());

        return ClassificationResult.builder()
                .transactionType(type)
                .category(category)
                .confidence(0.80)
                .source(ClassificationSource.PATTERN)
                .reason("Category keyword match: " + category.getDisplayName())
                .needsReview(false)
                .build();
    }

    @Override
    public int getPriority() {
        return 5;
    }

    private TransactionType inferTypeFromCategory(String categoryName, TransactionDirection direction) {
        if (direction == TransactionDirection.CREDIT) {
            return switch (categoryName) {
                case "SUBSCRIPTIONS" -> TransactionType.REFUND;
                default -> TransactionType.INCOME;
            };
        }
        return TransactionType.EXPENSE;
    }
}
