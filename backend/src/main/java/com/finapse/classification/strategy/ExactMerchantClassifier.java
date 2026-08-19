package com.finapse.classification.strategy;

import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.TransactionType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2) // 2nd priority
public class ExactMerchantClassifier implements ClassificationStrategy {

    @Override
    public boolean supports(ClassificationContext context) {
        return context.getMerchant() != null && context.getMerchant().getCategory() != null;
    }

    @Override
    public ClassificationResult classify(ClassificationContext context) {
        // If the merchant exists and has a predefined category, use it.
        // We assume generic expenses for standard merchants unless specified otherwise.
        
        return ClassificationResult.builder()
                .transactionType(TransactionType.EXPENSE) // Could be inferred from Category or DB
                .category(context.getMerchant().getCategory())
                .merchant(context.getMerchant())
                .confidence(0.98)
                .source(ClassificationSource.MERCHANT_DATABASE)
                .reason("Exact normalized merchant match with known category")
                .needsReview(false)
                .build();
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
