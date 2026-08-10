package com.finapse.service;

import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Classifies transactions based on description keywords, direction, and source.
 * Conservative: prefers UNKNOWN over incorrect classification.
 */
@Service
public class TransactionClassificationService {

    // Credit-card payment keywords (bank debit → paying a card bill)
    private static final List<String> CARD_PAYMENT_KEYWORDS = List.of(
            "CREDIT CARD", "CREDITCARD", "CARD PAYMENT", "CARD BILL",
            "CC PAYMENT", "CC BILL", "AMEX", "VISA PAYMENT", "MASTERCARD PAYMENT",
            "HDFC CARD", "ICICI CARD", "SBI CARD", "AXIS CARD", "KOTAK CARD",
            "CITI CARD", "INDUSIND CARD", "YES CARD", "RBL CARD"
    );

    private static final List<String> CASHBACK_KEYWORDS = List.of(
            "CASHBACK", "CASH BACK", "REWARD", "REWARDS", "POINTS REDEEMED",
            "LOYALTY CREDIT", "BONUS CREDIT"
    );

    private static final List<String> REFUND_KEYWORDS = List.of(
            "REFUND", "REVERSAL", "CHARGEBACK", "DISPUTE CREDIT",
            "RETURN CREDIT", "CANCELLED ORDER"
    );

    private static final List<String> TRANSFER_KEYWORDS = List.of(
            "NEFT", "RTGS", "IMPS", "UPI", "TRANSFER", "TRF",
            "FUND TRANSFER", "SELF TRANSFER", "INTERNAL TRANSFER"
    );

    private static final List<String> FEE_KEYWORDS = List.of(
            "ANNUAL FEE", "JOINING FEE", "LATE FEE", "PROCESSING FEE",
            "SERVICE CHARGE", "MAINTENANCE CHARGE", "PENALTY", "FINE",
            "CONVENIENCE FEE", "TRANSACTION FEE", "BANK CHARGE"
    );

    private static final List<String> INTEREST_KEYWORDS = List.of(
            "INTEREST", "FINANCE CHARGE", "OVERDUE INTEREST", "EMI INTEREST"
    );

    private static final List<String> INCOME_KEYWORDS = List.of(
            "SALARY", "PAYROLL", "WAGES", "DIVIDEND", "INTEREST CREDIT",
            "RENTAL INCOME", "FREELANCE", "CONSULTING", "BONUS", "STIPEND",
            "PENSION", "REIMBURSEMENT"
    );

    public TransactionType classify(Transaction tx) {
        String desc = tx.getDescription(); // already normalised (uppercase, trimmed)
        TransactionDirection dir = tx.getDirection();

        // Credits first — more deterministic
        if (dir == TransactionDirection.CREDIT) {
            if (containsAny(desc, CASHBACK_KEYWORDS)) return TransactionType.CASHBACK;
            if (containsAny(desc, REFUND_KEYWORDS))   return TransactionType.REFUND;
            if (containsAny(desc, INCOME_KEYWORDS))   return TransactionType.INCOME;
            if (containsAny(desc, INTEREST_KEYWORDS)) return TransactionType.INTEREST;
            if (containsAny(desc, TRANSFER_KEYWORDS)) return TransactionType.TRANSFER;
            // Generic credit from a bank account = likely income; from card = likely refund/cashback
            if (tx.getAccount() != null) return TransactionType.INCOME;
            return TransactionType.UNKNOWN;
        }

        // Debits
        if (dir == TransactionDirection.DEBIT) {
            if (containsAny(desc, CARD_PAYMENT_KEYWORDS)) return TransactionType.CREDIT_CARD_PAYMENT;
            if (containsAny(desc, FEE_KEYWORDS))          return TransactionType.FEE;
            if (containsAny(desc, INTEREST_KEYWORDS))     return TransactionType.INTEREST;
            if (containsAny(desc, TRANSFER_KEYWORDS))     return TransactionType.TRANSFER;
            // Generic debit = expense
            return TransactionType.EXPENSE;
        }

        return TransactionType.UNKNOWN;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
