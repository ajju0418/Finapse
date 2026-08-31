package com.finapse.service;

import com.finapse.dto.TrendResponse;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionType;
import com.finapse.exception.BadRequestException;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTrendsTest {

    @Mock TransactionRepository transactionRepository;
    @Mock ReconciliationReviewService reconciliationReviewService;
    @Mock UserService userService;
    @Mock AccountService accountService;
    @Mock CardService cardService;
    @InjectMocks DashboardService dashboardService;

    @Test
    void trends_producesOneBucketPerMonthEvenWhenAMonthIsEmpty() {
        when(userService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        TrendResponse response = dashboardService.getTrends(6, null, null);

        assertThat(response.points()).hasSize(6);
        assertThat(response.points()).allSatisfy(p ->
                assertThat(p.actualSpending()).isEqualByComparingTo("0"));
    }

    @Test
    void trends_subtractsRefundsFromSpendingAndIgnoresTransfers() {
        LocalDate thisMonth = YearMonth.from(LocalDate.now()).atDay(1);

        when(userService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(
                        tx(TransactionType.EXPENSE,  "5000.00", thisMonth),
                        tx(TransactionType.REFUND,   "1000.00", thisMonth),
                        tx(TransactionType.INCOME,  "50000.00", thisMonth),
                        // A card settlement is not new spending.
                        tx(TransactionType.CREDIT_CARD_PAYMENT, "5000.00", thisMonth)));

        TrendResponse response = dashboardService.getTrends(1, null, null);
        var point = response.points().get(0);

        assertThat(point.actualSpending()).isEqualByComparingTo("4000.00");
        assertThat(point.income()).isEqualByComparingTo("50000.00");
        assertThat(point.netCashFlow()).isEqualByComparingTo("46000.00");
    }

    @Test
    void trends_clampsAnAbsurdMonthCount() {
        when(userService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of());

        assertThat(dashboardService.getTrends(500, null, null).points()).hasSize(36);
        assertThat(dashboardService.getTrends(0, null, null).points()).hasSize(1);
    }

    @Test
    void dashboard_rejectsAReversedCustomRange() {
        assertThatThrownBy(() -> dashboardService.getDashboard(
                "CUSTOM", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be after");
    }

    @Test
    void dashboard_rejectsACustomPeriodWithOnlyOneDate() {
        assertThatThrownBy(() -> dashboardService.getDashboard(
                "CUSTOM", LocalDate.of(2026, 5, 1), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("both");
    }

    private Transaction tx(TransactionType type, String amount, LocalDate date) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setTransactionType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setTransactionDate(date);
        return tx;
    }
}
