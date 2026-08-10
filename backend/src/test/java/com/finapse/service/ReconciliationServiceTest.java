package com.finapse.service;

import com.finapse.entity.Account;
import com.finapse.entity.Card;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionLinkType;
import com.finapse.enums.TransactionType;
import com.finapse.repository.ReconciliationReviewRepository;
import com.finapse.repository.TransactionLinkRepository;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock TransactionLinkRepository transactionLinkRepository;
    @Mock ReconciliationReviewRepository reviewRepository;

    private ReconciliationService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(transactionRepository, transactionLinkRepository, reviewRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void cardPayment_matchingCardTransaction_createsReview() {
        Account account = account();
        Card card = card();

        Transaction bankPayment = tx(account, null, "SBI CREDIT CARD PAYMENT",
                LocalDate.of(2024, 2, 5), new BigDecimal("5000.00"), TransactionDirection.DEBIT, TransactionType.CREDIT_CARD_PAYMENT);

        Transaction cardTx = tx(null, card, "AMAZON PURCHASE",
                LocalDate.of(2024, 1, 20), new BigDecimal("5000.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE);

        when(transactionRepository.findByUserAndDateRange(eq(userId), any(), any()))
                .thenReturn(List.of(bankPayment, cardTx));
        when(transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(any(), any(), any()))
                .thenReturn(false);
        when(transactionLinkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.reconcile(List.of(bankPayment), userId);

        verify(transactionLinkRepository).save(argThat(link ->
                link.getLinkType() == TransactionLinkType.CREDIT_CARD_PAYMENT));
        verify(reviewRepository).save(any());
    }

    @Test
    void cardPayment_noMatchingCardTransaction_noReview() {
        Account account = account();

        Transaction bankPayment = tx(account, null, "SBI CREDIT CARD PAYMENT",
                LocalDate.of(2024, 2, 5), new BigDecimal("5000.00"), TransactionDirection.DEBIT, TransactionType.CREDIT_CARD_PAYMENT);

        Transaction otherBankTx = tx(account, null, "SWIGGY",
                LocalDate.of(2024, 1, 20), new BigDecimal("5000.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE);

        when(transactionRepository.findByUserAndDateRange(eq(userId), any(), any()))
                .thenReturn(List.of(bankPayment, otherBankTx));

        service.reconcile(List.of(bankPayment), userId);

        verify(transactionLinkRepository, never()).save(any());
    }

    @Test
    void refund_matchingDebit_createsReview() {
        Account account = account();

        Transaction purchase = tx(account, null, "AMAZON PURCHASE",
                LocalDate.of(2024, 1, 10), new BigDecimal("2000.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE);

        Transaction refund = tx(account, null, "REFUND FROM AMAZON",
                LocalDate.of(2024, 1, 20), new BigDecimal("2000.00"), TransactionDirection.CREDIT, TransactionType.REFUND);

        when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId()))
                .thenReturn(List.of(refund, purchase));
        when(transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(any(), any(), any()))
                .thenReturn(false);
        when(transactionLinkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.reconcile(List.of(refund), userId);

        verify(transactionLinkRepository).save(argThat(link ->
                link.getLinkType() == TransactionLinkType.REFUND));
        verify(reviewRepository).save(any());
    }

    @Test
    void cashback_smallCreditAfterDebit_createsReview() {
        Card card = card();

        Transaction purchase = tx(null, card, "AMAZON PURCHASE",
                LocalDate.of(2024, 1, 10), new BigDecimal("2000.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE);

        Transaction cashback = tx(null, card, "CASHBACK CREDIT",
                LocalDate.of(2024, 1, 15), new BigDecimal("100.00"), TransactionDirection.CREDIT, TransactionType.CASHBACK);

        when(transactionRepository.findByCardIdOrderByTransactionDateDesc(card.getId()))
                .thenReturn(List.of(cashback, purchase));
        when(transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(any(), any(), any()))
                .thenReturn(false);
        when(transactionLinkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.reconcile(List.of(cashback), userId);

        verify(transactionLinkRepository).save(argThat(link ->
                link.getLinkType() == TransactionLinkType.CASHBACK));
        verify(reviewRepository).save(any());
    }

    @Test
    void expense_noReconciliationAttempted() {
        Account account = account();
        Transaction expense = tx(account, null, "SWIGGY",
                LocalDate.of(2024, 1, 10), new BigDecimal("500.00"), TransactionDirection.DEBIT, TransactionType.EXPENSE);

        service.reconcile(List.of(expense), userId);

        verifyNoInteractions(transactionLinkRepository);
        verifyNoInteractions(reviewRepository);
    }

    // --- Helpers ---

    private Account account() {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        return a;
    }

    private Card card() {
        Card c = new Card();
        c.setId(UUID.randomUUID());
        return c;
    }

    private Transaction tx(Account account, Card card, String description,
                            LocalDate date, BigDecimal amount,
                            TransactionDirection direction, TransactionType type) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAccount(account);
        tx.setCard(card);
        tx.setDescription(description);
        tx.setTransactionDate(date);
        tx.setAmount(amount);
        tx.setDirection(direction);
        tx.setTransactionType(type);
        return tx;
    }
}
