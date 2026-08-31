package com.finapse.classification.strategy;

import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Order(6)
public class AmountPatternClassifier implements ClassificationStrategy {

    private static final BigDecimal VERIFICATION_THRESHOLD = new BigDecimal("10.00");
    private static final BigDecimal MICRO_AMOUNT = new BigDecimal("2.00");
    private static final Set<String> COMMON_VERIFICATION_AMOUNTS = Set.of(
            "1.00", "2.00", "5.00"
    );

    private static final Set<String> EMI_KEYWORDS = Set.of(
            "EMI", "INSTALMENT", "INSTALLMENT", "FLEXI PAY", "PAY LATER",
            "BAJAJ FINSERV", "ZEST MONEY", "LAZYPAY", "SIMPL"
    );

    private static final Set<String> SUBSCRIPTION_KEYWORDS = Set.of(
            "MONTHLY", "ANNUAL", "RENEWAL", "AUTO DEBIT", "RECURRING",
            "STANDING INSTRUCTION", "SI PAYMENT", "NACH", "ECS"
    );

    @Override
    public boolean supports(ClassificationContext context) {
        return context.getAmount() != null && context.getDirection() != null;
    }

    @Override
    public ClassificationResult classify(ClassificationContext context) {
        BigDecimal amount = context.getAmount();
        String narration = context.getNormalizedNarration() != null ? context.getNormalizedNarration() : "";

        ClassificationResult verificationResult = detectVerificationCharge(amount, context.getDirection());
        if (verificationResult != null) return verificationResult;

        ClassificationResult emiResult = detectEmi(narration, amount);
        if (emiResult != null) return emiResult;

        ClassificationResult subscriptionResult = detectSubscription(narration);
        if (subscriptionResult != null) return subscriptionResult;

        return null;
    }

    @Override
    public int getPriority() {
        return 6;
    }

    private ClassificationResult detectVerificationCharge(BigDecimal amount, TransactionDirection direction) {
        if (direction != TransactionDirection.DEBIT) return null;
        if (amount.compareTo(MICRO_AMOUNT) > 0) return null;

        if (COMMON_VERIFICATION_AMOUNTS.contains(amount.toPlainString())) {
            return ClassificationResult.builder()
                    .transactionType(TransactionType.VERIFICATION_CHARGE)
                    .confidence(0.75)
                    .source(ClassificationSource.PATTERN)
                    .reason("Micro-amount debit (" + amount + ") likely a verification charge")
                    .needsReview(true)
                    .build();
        }
        return null;
    }

    private ClassificationResult detectEmi(String narration, BigDecimal amount) {
        boolean hasEmiKeyword = EMI_KEYWORDS.stream().anyMatch(narration::contains);
        if (!hasEmiKeyword) return null;

        boolean isRoundAmount = amount.stripTrailingZeros().scale() <= 0
                || amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0;

        double confidence = isRoundAmount ? 0.88 : 0.80;

        return ClassificationResult.builder()
                .transactionType(TransactionType.EMI)
                .confidence(confidence)
                .source(ClassificationSource.PATTERN)
                .reason("EMI keyword detected" + (isRoundAmount ? " with round amount" : ""))
                .needsReview(false)
                .build();
    }

    private ClassificationResult detectSubscription(String narration) {
        boolean hasSubscriptionKeyword = SUBSCRIPTION_KEYWORDS.stream().anyMatch(narration::contains);
        if (!hasSubscriptionKeyword) return null;

        return ClassificationResult.builder()
                .transactionType(TransactionType.SUBSCRIPTION)
                .confidence(0.78)
                .source(ClassificationSource.PATTERN)
                .reason("Subscription/recurring keyword detected")
                .needsReview(false)
                .build();
    }
}
