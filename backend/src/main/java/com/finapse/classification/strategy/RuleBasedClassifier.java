package com.finapse.classification.strategy;

import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(4) // 4th priority, runs if exact matches fail
public class RuleBasedClassifier implements ClassificationStrategy {

    private static final List<String> CARD_PAYMENT_KEYWORDS = List.of(
            "CREDIT CARD", "CREDITCARD", "CARD PAYMENT", "CARD BILL",
            "CC PAYMENT", "CC BILL", "AMEX", "VISA PAYMENT", "MASTERCARD PAYMENT"
    );

    private static final List<String> CASHBACK_KEYWORDS = List.of(
            "CASHBACK", "CASH BACK", "REWARD", "REWARDS", "POINTS REDEEMED"
    );

    private static final List<String> REFUND_KEYWORDS = List.of(
            "REFUND", "REVERSAL", "CHARGEBACK", "DISPUTE CREDIT"
    );

    private static final List<String> FEE_KEYWORDS = List.of(
            "ANNUAL FEE", "JOINING FEE", "LATE FEE", "PROCESSING FEE",
            "SERVICE CHARGE", "MAINTENANCE CHARGE", "PENALTY", "FINE"
    );

    private static final List<String> INTEREST_KEYWORDS = List.of(
            "INTEREST", "FINANCE CHARGE", "OVERDUE INTEREST", "EMI INTEREST"
    );

    private static final List<String> INCOME_KEYWORDS = List.of(
            "SALARY", "SAL ", "-SAL-", "ACH ", "PAYROLL", "WAGES", "DIVIDEND", "INTEREST CREDIT",
            "RENTAL INCOME", "FREELANCE", "CONSULTING", "BONUS", "STIPEND",
            "PENSION", "REIMBURSEMENT", "NEFT CR-CHAS"
    );

    private static final List<String> TRANSFER_KEYWORDS = List.of(
            "TRANSFER", "TRF", "FUND TRANSFER", "SELF TRANSFER", "INTERNAL TRANSFER", "TO OWN A/C"
    );

    @Override
    public boolean supports(ClassificationContext context) {
        return context.getNormalizedNarration() != null;
    }

    @Override
    public ClassificationResult classify(ClassificationContext context) {
        String desc = context.getNormalizedNarration();
        TransactionDirection dir = context.getDirection();

        // Credits
        if (dir == TransactionDirection.CREDIT) {
            if (desc.contains("REVERSAL") && containsAny(desc, CASHBACK_KEYWORDS)) {
                return buildResult(TransactionType.REFUND, 0.95, "Cashback reversal");
            }
            if (containsAny(desc, CASHBACK_KEYWORDS)) return buildResult(TransactionType.CASHBACK, 0.95, "Cashback keywords found");
            if (containsAny(desc, REFUND_KEYWORDS)) return buildResult(TransactionType.REFUND, 0.95, "Refund keywords found");
            if (containsAny(desc, INCOME_KEYWORDS)) return buildResult(TransactionType.INCOME, 0.90, "Income keywords found");
            if (containsAny(desc, INTEREST_KEYWORDS)) return buildResult(TransactionType.INTEREST, 0.90, "Interest keywords found");
            if (containsAny(desc, TRANSFER_KEYWORDS)) return buildResult(TransactionType.TRANSFER, 0.85, "Explicit transfer keywords found");
            
            // Generic credit
            return buildResult(TransactionType.INCOME, 0.60, "Generic credit defaulting to income");
        }

        // Debits
        if (dir == TransactionDirection.DEBIT) {
            if (containsAny(desc, CARD_PAYMENT_KEYWORDS)) return buildResult(TransactionType.CREDIT_CARD_PAYMENT, 0.95, "Card payment keywords found");
            if (containsAny(desc, FEE_KEYWORDS)) return buildResult(TransactionType.FEE, 0.90, "Fee keywords found");
            if (containsAny(desc, INTEREST_KEYWORDS)) return buildResult(TransactionType.INTEREST, 0.90, "Interest keywords found");
            if (containsAny(desc, TRANSFER_KEYWORDS)) return buildResult(TransactionType.TRANSFER, 0.85, "Explicit transfer keywords found");
            
            // Generic debit
            return buildResult(TransactionType.EXPENSE, 0.60, "Generic debit defaulting to expense");
        }

        return null; // Let orchestrator handle default
    }

    private ClassificationResult buildResult(TransactionType type, double confidence, String reason) {
        return ClassificationResult.builder()
                .transactionType(type)
                .confidence(confidence)
                .source(ClassificationSource.EXACT_RULE)
                .reason(reason)
                .needsReview(confidence < 0.75)
                .build();
    }

    @Override
    public int getPriority() {
        return 4;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
