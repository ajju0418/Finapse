package com.finapse.service;

import com.finapse.dto.BudgetRequest;
import com.finapse.dto.BudgetResponse;
import com.finapse.entity.Budget;
import com.finapse.entity.Category;
import com.finapse.entity.Transaction;
import com.finapse.entity.User;
import com.finapse.enums.BudgetPeriod;
import com.finapse.enums.BudgetStatus;
import com.finapse.enums.TransactionType;
import com.finapse.exception.ConflictException;
import com.finapse.repository.BudgetRepository;
import com.finapse.repository.CategoryRepository;
import com.finapse.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock BudgetRepository budgetRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock UserService userService;
    @InjectMocks BudgetService budgetService;

    private User user;
    private Category groceries;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        groceries = new Category();
        groceries.setId(UUID.randomUUID());
        groceries.setName("GROCERIES");
    }

    @Test
    void getAll_countsExpensesAndSubtractsRefunds() {
        Budget budget = budget(groceries, "10000.00");
        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(budgetRepository.findActiveByUser(user.getId())).thenReturn(List.of(budget));
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(
                        tx(TransactionType.EXPENSE, "3000.00", groceries),
                        tx(TransactionType.EXPENSE, "1000.00", groceries),
                        tx(TransactionType.REFUND,  "500.00",  groceries),
                        // Belongs to another category and must be ignored.
                        tx(TransactionType.EXPENSE, "9000.00", null)));

        BudgetResponse response = budgetService.getAll().get(0);

        assertThat(response.spent()).isEqualByComparingTo("3500.00");
        assertThat(response.remaining()).isEqualByComparingTo("6500.00");
        assertThat(response.status()).isEqualTo(BudgetStatus.ON_TRACK);
    }

    @Test
    void getAll_overallBudgetIncludesEveryCategory() {
        Budget overall = budget(null, "10000.00");
        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(budgetRepository.findActiveByUser(user.getId())).thenReturn(List.of(overall));
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(
                        tx(TransactionType.EXPENSE, "3000.00", groceries),
                        tx(TransactionType.EXPENSE, "2000.00", null),
                        // Card payments settle earlier spending; they are not new spending.
                        tx(TransactionType.CREDIT_CARD_PAYMENT, "8000.00", null)));

        BudgetResponse response = budgetService.getAll().get(0);

        assertThat(response.spent()).isEqualByComparingTo("5000.00");
    }

    @Test
    void getAll_flagsExceededWhenSpendPassesTheLimit() {
        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(budgetRepository.findActiveByUser(user.getId()))
                .thenReturn(List.of(budget(groceries, "1000.00")));
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(tx(TransactionType.EXPENSE, "1200.00", groceries)));

        BudgetResponse response = budgetService.getAll().get(0);

        assertThat(response.status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(response.percentUsed()).isEqualTo(120.0);
        assertThat(response.remaining()).isEqualByComparingTo("-200.00");
    }

    @Test
    void getAll_flagsAtRiskOnceTheAlertThresholdIsReached() {
        Budget budget = budget(groceries, "1000.00");
        budget.setAlertThreshold(80);

        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(budgetRepository.findActiveByUser(user.getId())).thenReturn(List.of(budget));
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(tx(TransactionType.EXPENSE, "850.00", groceries)));

        assertThat(budgetService.getAll().get(0).status()).isEqualTo(BudgetStatus.AT_RISK);
    }

    @Test
    void getAll_neverReportsNegativeSpendWhenRefundsExceedExpenses() {
        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(budgetRepository.findActiveByUser(user.getId()))
                .thenReturn(List.of(budget(groceries, "1000.00")));
        when(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                .thenReturn(List.of(
                        tx(TransactionType.EXPENSE, "100.00", groceries),
                        tx(TransactionType.REFUND,  "400.00", groceries)));

        assertThat(budgetService.getAll().get(0).spent()).isEqualByComparingTo("0");
    }

    @Test
    void create_rejectsASecondBudgetForTheSameCategoryAndPeriod() {
        BudgetRequest request = new BudgetRequest(
                groceries.getId(), new BigDecimal("5000"), BudgetPeriod.MONTHLY, 80);

        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(budgetRepository.findActiveByUserCategoryAndPeriod(
                user.getId(), groceries.getId(), BudgetPeriod.MONTHLY))
                .thenReturn(Optional.of(budget(groceries, "5000.00")));

        assertThatThrownBy(() -> budgetService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getAll_returnsEmptyWithoutQueryingTransactions() {
        when(userService.getCurrentUserId()).thenReturn(user.getId());
        when(budgetRepository.findActiveByUser(user.getId())).thenReturn(List.of());

        assertThat(budgetService.getAll()).isEmpty();
    }

    // ---------------------------------------------------------------------

    private Budget budget(Category category, String limit) {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setUser(user);
        budget.setCategory(category);
        budget.setLimitAmount(new BigDecimal(limit));
        budget.setPeriod(BudgetPeriod.MONTHLY);
        budget.setAlertThreshold(80);
        return budget;
    }

    private Transaction tx(TransactionType type, String amount, Category category) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setTransactionType(type);
        tx.setAmount(new BigDecimal(amount));
        tx.setCategory(category);
        tx.setTransactionDate(LocalDate.now());
        return tx;
    }
}
