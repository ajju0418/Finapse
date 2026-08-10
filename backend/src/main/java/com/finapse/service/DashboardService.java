package com.finapse.service;

import com.finapse.dto.*;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionType;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationReviewService reconciliationReviewService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String period) {
        UUID userId = userService.getDefaultUser().getId();
        LocalDate[] range = resolvePeriod(period);
        LocalDate from = range[0];
        LocalDate to = range[1];

        BigDecimal income = sum(userId, from, to, TransactionType.INCOME);
        BigDecimal grossExpenses = sum(userId, from, to, TransactionType.EXPENSE);
        BigDecimal refunds = sum(userId, from, to, TransactionType.REFUND);
        BigDecimal cashback = sum(userId, from, to, TransactionType.CASHBACK);
        BigDecimal actualSpending = grossExpenses.subtract(refunds).max(BigDecimal.ZERO);
        BigDecimal netCashFlow = income.subtract(actualSpending);

        List<Transaction> allInPeriod = transactionRepository.findByUserAndDateRange(userId, from, to);

        List<CategorySpendingDto> categoryBreakdown = buildCategoryBreakdown(allInPeriod, grossExpenses);
        List<MerchantSpendingDto> topMerchants = buildTopMerchants(allInPeriod);
        List<TransactionResponse> recent = transactionRepository.findRecentByUser(userId, 10)
                .stream().map(TransactionResponse::from).toList();

        long pendingReviews = reconciliationReviewService.countPending();

        return new DashboardResponse(
                income, grossExpenses, refunds, actualSpending, cashback, netCashFlow,
                from.toString(), to.toString(),
                categoryBreakdown, topMerchants, recent, pendingReviews
        );
    }

    // -------------------------------------------------------------------------

    private BigDecimal sum(UUID userId, LocalDate from, LocalDate to, TransactionType type) {
        BigDecimal result = transactionRepository.sumAmountByUserAndDateRangeAndType(userId, from, to, type);
        return result != null ? result : BigDecimal.ZERO;
    }

    private List<CategorySpendingDto> buildCategoryBreakdown(List<Transaction> transactions, BigDecimal totalExpenses) {
        if (totalExpenses.compareTo(BigDecimal.ZERO) == 0) return List.of();

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            if (tx.getTransactionType() != TransactionType.EXPENSE) continue;
            String cat = tx.getCategory() != null ? tx.getCategory().getName() : "Other";
            byCategory.merge(cat, tx.getAmount(), BigDecimal::add);
        }

        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategorySpendingDto(
                        e.getKey(),
                        e.getValue(),
                        e.getValue().multiply(BigDecimal.valueOf(100))
                                .divide(totalExpenses, 1, RoundingMode.HALF_UP)
                                .doubleValue()
                ))
                .toList();
    }

    private List<MerchantSpendingDto> buildTopMerchants(List<Transaction> transactions) {
        record MerchantAgg(BigDecimal total, int count) {}

        Map<String, MerchantAgg> byMerchant = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            if (tx.getTransactionType() != TransactionType.EXPENSE) continue;
            if (tx.getMerchant() == null) continue;
            String name = tx.getMerchant().getName();
            byMerchant.merge(name,
                    new MerchantAgg(tx.getAmount(), 1),
                    (a, b) -> new MerchantAgg(a.total().add(b.total()), a.count() + b.count()));
        }

        return byMerchant.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, MerchantAgg> e) -> e.getValue().total()).reversed())
                .limit(10)
                .map(e -> new MerchantSpendingDto(e.getKey(), e.getValue().total(), e.getValue().count()))
                .toList();
    }

    /**
     * Resolves a period string to [from, to] date range.
     */
    private LocalDate[] resolvePeriod(String period) {
        LocalDate today = LocalDate.now();
        return switch (period == null ? "THIS_MONTH" : period.toUpperCase()) {
            case "7_DAYS"     -> new LocalDate[]{ today.minusDays(6), today };
            case "30_DAYS"    -> new LocalDate[]{ today.minusDays(29), today };
            case "3_MONTHS"   -> new LocalDate[]{ today.minusMonths(3).withDayOfMonth(1), today };
            case "6_MONTHS"   -> new LocalDate[]{ today.minusMonths(6).withDayOfMonth(1), today };
            case "1_YEAR"     -> new LocalDate[]{ today.minusYears(1).withDayOfMonth(1), today };
            default           -> { // THIS_MONTH
                YearMonth ym = YearMonth.from(today);
                yield new LocalDate[]{ ym.atDay(1), ym.atEndOfMonth() };
            }
        };
    }
}
