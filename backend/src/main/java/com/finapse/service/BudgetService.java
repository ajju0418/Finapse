package com.finapse.service;

import com.finapse.dto.BudgetRequest;
import com.finapse.dto.BudgetResponse;
import com.finapse.entity.Budget;
import com.finapse.entity.Category;
import com.finapse.entity.Transaction;
import com.finapse.enums.BudgetPeriod;
import com.finapse.enums.BudgetStatus;
import com.finapse.enums.TransactionType;
import com.finapse.exception.ConflictException;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.BudgetRepository;
import com.finapse.repository.CategoryRepository;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Budgets compare actual spending against a user-defined cap for the current
 * period. Spending is measured the same way the dashboard measures it:
 * EXPENSE minus REFUND, floored at zero.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    /** Per-period spend rollup so N budgets never trigger N transaction queries. */
    private record PeriodSpend(BigDecimal overall, Map<UUID, BigDecimal> byCategory) {}

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAll() {
        UUID userId = userService.getCurrentUserId();
        List<Budget> budgets = budgetRepository.findActiveByUser(userId);
        if (budgets.isEmpty()) return List.of();

        Map<BudgetPeriod, PeriodSpend> spendByPeriod = new HashMap<>();
        for (BudgetPeriod period : budgets.stream().map(Budget::getPeriod).distinct().toList()) {
            LocalDate[] range = resolveRange(period);
            spendByPeriod.put(period, aggregate(userId, range));
        }

        List<BudgetResponse> result = new ArrayList<>();
        for (Budget budget : budgets) {
            LocalDate[] range = resolveRange(budget.getPeriod());
            PeriodSpend rollup = spendByPeriod.get(budget.getPeriod());
            BigDecimal spent = budget.getCategory() == null
                    ? rollup.overall()
                    : rollup.byCategory()
                            .getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO)
                            .max(BigDecimal.ZERO);
            result.add(toResponse(budget, range[0], range[1], spent));
        }
        return result;
    }

    private PeriodSpend aggregate(UUID userId, LocalDate[] range) {
        Map<UUID, BigDecimal> byCategory = new HashMap<>();
        BigDecimal overall = BigDecimal.ZERO;
        for (Transaction tx : transactionRepository.findByUserAndDateRange(userId, range[0], range[1])) {
            BigDecimal signed = signedSpend(tx);
            if (signed == null) continue;
            overall = overall.add(signed);
            if (tx.getCategory() != null) {
                byCategory.merge(tx.getCategory().getId(), signed, BigDecimal::add);
            }
        }
        return new PeriodSpend(overall.max(BigDecimal.ZERO), byCategory);
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        UUID userId = userService.getCurrentUserId();
        BudgetPeriod period = request.periodOrDefault();

        budgetRepository.findActiveByUserCategoryAndPeriod(userId, request.categoryId(), period)
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "A " + period.name().toLowerCase() + " budget already exists for this category. "
                            + "Edit the existing budget instead.");
                });

        Budget budget = new Budget();
        budget.setUser(userService.getCurrentUser());
        budget.setCategory(resolveCategory(request.categoryId()));
        budget.setLimitAmount(request.limitAmount());
        budget.setPeriod(period);
        budget.setAlertThreshold(request.alertThresholdOrDefault());
        budget = budgetRepository.save(budget);

        LocalDate[] range = resolveRange(period);
        return toResponse(budget, range[0], range[1], spentFor(userId, budget, range));
    }

    @Transactional
    public BudgetResponse update(UUID id, BudgetRequest request) {
        UUID userId = userService.getCurrentUserId();
        Budget budget = findOrThrow(id, userId);
        budget.setCategory(resolveCategory(request.categoryId()));
        budget.setLimitAmount(request.limitAmount());
        budget.setPeriod(request.periodOrDefault());
        budget.setAlertThreshold(request.alertThresholdOrDefault());
        budget = budgetRepository.save(budget);

        LocalDate[] range = resolveRange(budget.getPeriod());
        return toResponse(budget, range[0], range[1], spentFor(userId, budget, range));
    }

    @Transactional
    public void delete(UUID id) {
        Budget budget = findOrThrow(id, userService.getCurrentUserId());
        budgetRepository.delete(budget);
    }

    // -------------------------------------------------------------------------

    private Budget findOrThrow(UUID id, UUID userId) {
        return budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private BigDecimal spentFor(UUID userId, Budget budget, LocalDate[] range) {
        BigDecimal total = BigDecimal.ZERO;
        UUID targetCategory = budget.getCategory() != null ? budget.getCategory().getId() : null;
        for (Transaction tx : transactionRepository.findByUserAndDateRange(userId, range[0], range[1])) {
            BigDecimal signed = signedSpend(tx);
            if (signed == null) continue;
            if (targetCategory != null) {
                if (tx.getCategory() == null || !targetCategory.equals(tx.getCategory().getId())) continue;
            }
            total = total.add(signed);
        }
        return total.max(BigDecimal.ZERO);
    }

    /** Expenses count positive, refunds count negative, everything else is ignored. */
    private BigDecimal signedSpend(Transaction tx) {
        if (tx.getTransactionType() == TransactionType.EXPENSE) return tx.getAmount();
        if (tx.getTransactionType() == TransactionType.REFUND) return tx.getAmount().negate();
        return null;
    }

    private BudgetResponse toResponse(Budget budget, LocalDate from, LocalDate to, BigDecimal spent) {
        BigDecimal limit = budget.getLimitAmount();
        BigDecimal remaining = limit.subtract(spent);
        double percentUsed = limit.compareTo(BigDecimal.ZERO) == 0
                ? 0d
                : spent.multiply(BigDecimal.valueOf(100))
                       .divide(limit, 1, RoundingMode.HALF_UP)
                       .doubleValue();

        BudgetStatus status;
        if (percentUsed >= 100d) status = BudgetStatus.EXCEEDED;
        else if (percentUsed >= budget.getAlertThreshold()) status = BudgetStatus.AT_RISK;
        else status = BudgetStatus.ON_TRACK;

        BigDecimal projected = project(spent, from, to);

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory() != null ? budget.getCategory().getId() : null,
                budget.getCategory() != null ? budget.getCategory().getName() : null,
                limit,
                budget.getPeriod(),
                budget.getAlertThreshold(),
                from, to,
                spent,
                remaining,
                percentUsed,
                status,
                projected,
                projected.compareTo(limit) > 0
        );
    }

    /** Straight-line run rate: spend-to-date scaled to the full period length. */
    private BigDecimal project(BigDecimal spent, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (today.isAfter(to)) return spent;
        long elapsed = ChronoUnit.DAYS.between(from, today) + 1;
        long total = ChronoUnit.DAYS.between(from, to) + 1;
        if (elapsed <= 0 || total <= 0) return spent;
        return spent.multiply(BigDecimal.valueOf(total))
                    .divide(BigDecimal.valueOf(elapsed), 2, RoundingMode.HALF_UP);
    }

    private LocalDate[] resolveRange(BudgetPeriod period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case WEEKLY -> {
                LocalDate start = today.with(DayOfWeek.MONDAY);
                yield new LocalDate[]{ start, start.plusDays(6) };
            }
            case YEARLY -> new LocalDate[]{ today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()) };
            case MONTHLY -> {
                YearMonth ym = YearMonth.from(today);
                yield new LocalDate[]{ ym.atDay(1), ym.atEndOfMonth() };
            }
        };
    }
}
