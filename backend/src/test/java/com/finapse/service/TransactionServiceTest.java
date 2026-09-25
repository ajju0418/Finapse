package com.finapse.service;

import com.finapse.classification.detection.NormalizationService;
import com.finapse.dto.PageResponse;
import com.finapse.dto.TransactionResponse;
import com.finapse.entity.Statement;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock CategoryService categoryService;
    @Mock UserService userService;
    @Mock UserClassificationRuleService ruleService;
    @Mock NormalizationService normalizationService;
    @InjectMocks TransactionService transactionService;

    @Test
    void getByStatement_returnsPageWithMetadata() {
        UUID statementId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<Transaction> page = new PageImpl<>(
                List.of(tx("2000.00"), tx("1500.00")), pageable, 5);
        when(transactionRepository.findByStatementId(eq(statementId), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<TransactionResponse> response = transactionService.getByStatement(statementId, pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void getByCard_emptyPage_reportsZeroTotals() {
        UUID cardId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 25);
        when(transactionRepository.findByCardId(eq(cardId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<TransactionResponse> response = transactionService.getByCard(cardId, pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void getByAccount_dropsUnknownSortColumn_andFallsBackToDefault() {
        UUID accountId = UUID.randomUUID();
        // A client asking to sort by a non-allowlisted property must not reach the DB.
        Pageable malicious = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "statement.user.email"));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(transactionRepository.findByAccountId(eq(accountId), captor.capture()))
                .thenReturn(new PageImpl<>(List.of(), malicious, 0));

        transactionService.getByAccount(accountId, malicious);

        Pageable sanitized = captor.getValue();
        assertThat(sanitized.getPageNumber()).isEqualTo(1);
        assertThat(sanitized.getPageSize()).isEqualTo(10);
        assertThat(sanitized.getSort().getOrderFor("statement.user.email")).isNull();
        assertThat(sanitized.getSort().getOrderFor("transactionDate")).isNotNull();
    }

    @Test
    void getByAccount_keepsAllowlistedSortColumn() {
        UUID accountId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.ASC, "amount"));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(transactionRepository.findByAccountId(eq(accountId), captor.capture()))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        transactionService.getByAccount(accountId, pageable);

        Sort.Order amount = captor.getValue().getSort().getOrderFor("amount");
        assertThat(amount).isNotNull();
        assertThat(amount.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    private Transaction tx(String amount) {
        Statement statement = new Statement();
        statement.setId(UUID.randomUUID());

        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setStatement(statement);
        tx.setTransactionDate(LocalDate.now());
        tx.setDescription("Test");
        tx.setAmount(new BigDecimal(amount));
        tx.setDirection(TransactionDirection.DEBIT);
        tx.setTransactionType(TransactionType.EXPENSE);
        return tx;
    }
}
