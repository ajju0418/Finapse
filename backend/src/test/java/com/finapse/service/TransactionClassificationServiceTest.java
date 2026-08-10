package com.finapse.service;

import com.finapse.entity.Account;
import com.finapse.entity.Card;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionClassificationServiceTest {

    private TransactionClassificationService service;

    @BeforeEach
    void setUp() {
        service = new TransactionClassificationService();
    }

    // --- DEBIT classifications ---

    @Test
    void debit_genericDescription_classifiedAsExpense() {
        assertThat(classify("SWIGGY ORDER 123", TransactionDirection.DEBIT, true))
                .isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void debit_creditCardKeyword_classifiedAsCardPayment() {
        assertThat(classify("SBI CREDIT CARD PAYMENT", TransactionDirection.DEBIT, true))
                .isEqualTo(TransactionType.CREDIT_CARD_PAYMENT);
    }

    @Test
    void debit_cardBillKeyword_classifiedAsCardPayment() {
        assertThat(classify("HDFC CARD BILL PAYMENT", TransactionDirection.DEBIT, true))
                .isEqualTo(TransactionType.CREDIT_CARD_PAYMENT);
    }

    @Test
    void debit_annualFee_classifiedAsFee() {
        assertThat(classify("ANNUAL FEE CHARGED", TransactionDirection.DEBIT, true))
                .isEqualTo(TransactionType.FEE);
    }

    @Test
    void debit_neft_classifiedAsTransfer() {
        assertThat(classify("NEFT TO JOHN DOE", TransactionDirection.DEBIT, true))
                .isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void debit_interest_classifiedAsInterest() {
        assertThat(classify("FINANCE CHARGE ON OUTSTANDING", TransactionDirection.DEBIT, true))
                .isEqualTo(TransactionType.INTEREST);
    }

    // --- CREDIT classifications ---

    @Test
    void credit_salaryKeyword_classifiedAsIncome() {
        assertThat(classify("SALARY CREDIT JULY", TransactionDirection.CREDIT, true))
                .isEqualTo(TransactionType.INCOME);
    }

    @Test
    void credit_cashbackKeyword_classifiedAsCashback() {
        assertThat(classify("CASHBACK CREDIT", TransactionDirection.CREDIT, false))
                .isEqualTo(TransactionType.CASHBACK);
    }

    @Test
    void credit_refundKeyword_classifiedAsRefund() {
        assertThat(classify("REFUND FROM AMAZON", TransactionDirection.CREDIT, false))
                .isEqualTo(TransactionType.REFUND);
    }

    @Test
    void credit_genericBankCredit_classifiedAsIncome() {
        assertThat(classify("SOME RANDOM CREDIT", TransactionDirection.CREDIT, true))
                .isEqualTo(TransactionType.INCOME);
    }

    @Test
    void credit_genericCardCredit_classifiedAsUnknown() {
        assertThat(classify("SOME RANDOM CREDIT", TransactionDirection.CREDIT, false))
                .isEqualTo(TransactionType.UNKNOWN);
    }

    @Test
    void credit_upiTransfer_classifiedAsTransfer() {
        assertThat(classify("UPI TRANSFER FROM FRIEND", TransactionDirection.CREDIT, true))
                .isEqualTo(TransactionType.TRANSFER);
    }

    // --- Helper ---

    private TransactionType classify(String description, TransactionDirection direction, boolean isAccount) {
        Transaction tx = new Transaction();
        tx.setDescription(description);
        tx.setDirection(direction);
        if (isAccount) tx.setAccount(new Account());
        else tx.setCard(new Card());
        return service.classify(tx);
    }
}
