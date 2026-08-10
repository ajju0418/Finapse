package com.finapse.service;

import com.finapse.entity.Account;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionLinkType;
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
class DuplicateDetectionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock TransactionLinkRepository transactionLinkRepository;
    @Mock ReconciliationReviewRepository reviewRepository;

    private DuplicateDetectionService service;

    @BeforeEach
    void setUp() {
        service = new DuplicateDetectionService(transactionRepository, transactionLinkRepository, reviewRepository);
    }

    @Test
    void exactHashMatch_createsReview() {
        Account account = account();
        Transaction tx = tx(account, "HASH123", LocalDate.of(2024, 1, 10), new BigDecimal("500.00"), TransactionDirection.DEBIT);
        Transaction existing = tx(account, "HASH123", LocalDate.of(2024, 1, 10), new BigDecimal("500.00"), TransactionDirection.DEBIT);

        when(transactionRepository.findByTransactionHashAndIdNot("HASH123", tx.getId()))
                .thenReturn(List.of(existing));
        when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId()))
                .thenReturn(List.of(tx, existing));
        when(transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(any(), any(), any()))
                .thenReturn(false);
        when(transactionLinkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.detectDuplicates(List.of(tx));

        verify(transactionLinkRepository, atLeastOnce()).save(argThat(link ->
                link.getLinkType() == TransactionLinkType.DUPLICATE));
        verify(reviewRepository, atLeastOnce()).save(any());
    }

    @Test
    void noHashMatch_noReviewCreated() {
        Account account = account();
        Transaction tx = tx(account, "HASH_A", LocalDate.of(2024, 1, 10), new BigDecimal("500.00"), TransactionDirection.DEBIT);

        when(transactionRepository.findByTransactionHashAndIdNot("HASH_A", tx.getId()))
                .thenReturn(List.of());
        when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId()))
                .thenReturn(List.of(tx));

        service.detectDuplicates(List.of(tx));

        verify(transactionLinkRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void nearDuplicate_sameAmountAndDirectionWithin3Days_createsReview() {
        Account account = account();
        Transaction tx = tx(account, "HASH_A", LocalDate.of(2024, 1, 10), new BigDecimal("1000.00"), TransactionDirection.DEBIT);
        Transaction near = tx(account, "HASH_B", LocalDate.of(2024, 1, 11), new BigDecimal("1000.00"), TransactionDirection.DEBIT);

        when(transactionRepository.findByTransactionHashAndIdNot(anyString(), any())).thenReturn(List.of());
        when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId()))
                .thenReturn(List.of(tx, near));
        when(transactionLinkRepository.existsBySourceTransactionIdAndTargetTransactionIdAndLinkType(any(), any(), any()))
                .thenReturn(false);
        when(transactionLinkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.detectDuplicates(List.of(tx));

        verify(transactionLinkRepository, atLeastOnce()).save(any());
    }

    @Test
    void nearDuplicate_differentAmount_noReview() {
        Account account = account();
        Transaction tx = tx(account, "HASH_A", LocalDate.of(2024, 1, 10), new BigDecimal("1000.00"), TransactionDirection.DEBIT);
        Transaction other = tx(account, "HASH_B", LocalDate.of(2024, 1, 10), new BigDecimal("999.00"), TransactionDirection.DEBIT);

        when(transactionRepository.findByTransactionHashAndIdNot(anyString(), any())).thenReturn(List.of());
        when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId()))
                .thenReturn(List.of(tx, other));

        service.detectDuplicates(List.of(tx));

        verify(transactionLinkRepository, never()).save(any());
    }

    // --- Helpers ---

    private Account account() {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        return a;
    }

    private Transaction tx(Account account, String hash, LocalDate date, BigDecimal amount, TransactionDirection dir) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setAccount(account);
        tx.setTransactionHash(hash);
        tx.setTransactionDate(date);
        tx.setAmount(amount);
        tx.setDirection(dir);
        tx.setDescription("TEST DESCRIPTION");
        return tx;
    }
}
