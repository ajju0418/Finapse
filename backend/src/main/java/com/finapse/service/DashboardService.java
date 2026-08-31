package com.finapse.service;

import com.finapse.dto.*;
import com.finapse.entity.Transaction;
import com.finapse.enums.TransactionType;
import com.finapse.exception.BadRequestException;
import com.finapse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationReviewService reconciliationReviewService;
    private final UserService userService;
    private final AccountService accountService;
    private final CardService cardService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String period) {
        return getDashboard(period, null, null);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String period, LocalDate customFrom, LocalDate customTo) {
        UUID userId = userService.getCurrentUserId();
        LocalDate[] range = resolveRange(period, customFrom, customTo);
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

        // Source Summaries
        List<FinancialSourceSummary> sourceSummaries = new ArrayList<>();

        accountService.getAll().forEach(acc -> {
            var analytics = accountService.getAnalytics(acc.id());
            sourceSummaries.add(new FinancialSourceSummary(
                    acc.id(), acc.name(), acc.institutionName(),
                    analytics.netChange(), analytics.totalOutflow(), false
            ));
        });

        cardService.getAll().forEach(card -> {
            var analytics = cardService.getAnalytics(card.id());
            sourceSummaries.add(new FinancialSourceSummary(
                    card.id(), card.name(), card.issuer(),
                    analytics.outstanding(), analytics.totalSpending(), true
            ));
        });

        return new DashboardResponse(
                income, grossExpenses, refunds, actualSpending, cashback, netCashFlow,
                from.toString(), to.toString(),
                categoryBreakdown, topMerchants, recent, pendingReviews,
                sourceSummaries
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
     * Month-by-month income / spending / net cash-flow series, oldest bucket first.
     * Backs the trend chart and month-over-month comparison.
     */
    @Transactional(readOnly = true)
    public TrendResponse getTrends(int months, LocalDate customFrom, LocalDate customTo) {
        UUID userId = userService.getCurrentUserId();

        LocalDate to;
        LocalDate from;
        if (customFrom != null && customTo != null) {
            validateCustomRange(customFrom, customTo);
            from = customFrom.withDayOfMonth(1);
            to = YearMonth.from(customTo).atEndOfMonth();
        } else {
            int span = Math.min(Math.max(months, 1), 36);
            YearMonth current = YearMonth.from(LocalDate.now());
            from = current.minusMonths(span - 1L).atDay(1);
            to = current.atEndOfMonth();
        }

        Map<YearMonth, BigDecimal[]> buckets = new LinkedHashMap<>();
        Map<YearMonth, Integer> counts = new LinkedHashMap<>();
        for (YearMonth ym = YearMonth.from(from); !ym.isAfter(YearMonth.from(to)); ym = ym.plusMonths(1)) {
            buckets.put(ym, new BigDecimal[]{ BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            counts.put(ym, 0);
        }

        for (Transaction tx : transactionRepository.findByUserAndDateRange(userId, from, to)) {
            YearMonth ym = YearMonth.from(tx.getTransactionDate());
            BigDecimal[] slot = buckets.get(ym);
            if (slot == null) continue;
            counts.merge(ym, 1, Integer::sum);
            switch (tx.getTransactionType()) {
                case INCOME   -> slot[0] = slot[0].add(tx.getAmount());
                case EXPENSE  -> slot[1] = slot[1].add(tx.getAmount());
                case REFUND   -> slot[2] = slot[2].add(tx.getAmount());
                case CASHBACK -> slot[3] = slot[3].add(tx.getAmount());
                default -> { /* transfers, card payments and fees are not spending */ }
            }
        }

        List<TrendPointDto> points = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal[]> entry : buckets.entrySet()) {
            BigDecimal[] slot = entry.getValue();
            BigDecimal actual = slot[1].subtract(slot[2]).max(BigDecimal.ZERO);
            points.add(new TrendPointDto(
                    entry.getKey().toString(),
                    entry.getKey().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                            + " " + entry.getKey().getYear(),
                    slot[0], slot[1], slot[2], actual, slot[3],
                    slot[0].subtract(actual),
                    counts.getOrDefault(entry.getKey(), 0)
            ));
        }

        BigDecimal changeAmount = BigDecimal.ZERO;
        Double changePercent = null;
        if (points.size() >= 2) {
            BigDecimal latest = points.get(points.size() - 1).actualSpending();
            BigDecimal previous = points.get(points.size() - 2).actualSpending();
            changeAmount = latest.subtract(previous);
            if (previous.compareTo(BigDecimal.ZERO) > 0) {
                changePercent = changeAmount.multiply(BigDecimal.valueOf(100))
                        .divide(previous, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }
        }

        int divisor = Math.max(points.size(), 1);
        BigDecimal avgSpend = points.stream().map(TrendPointDto::actualSpending)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
        BigDecimal avgIncome = points.stream().map(TrendPointDto::income)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);

        return new TrendResponse(from.toString(), to.toString(), "MONTHLY", points,
                changeAmount, changePercent, avgSpend, avgIncome);
    }

    /**
     * Resolves a period string to [from, to]. An explicit from/to pair always wins,
     * which is what powers the custom date-range picker.
     */
    private LocalDate[] resolveRange(String period, LocalDate customFrom, LocalDate customTo) {
        if (customFrom != null || customTo != null || "CUSTOM".equalsIgnoreCase(period)) {
            if (customFrom == null || customTo == null) {
                throw new BadRequestException(
                        "A custom period needs both a 'from' and a 'to' date.");
            }
            validateCustomRange(customFrom, customTo);
            return new LocalDate[]{ customFrom, customTo };
        }
        LocalDate today = LocalDate.now();
        return switch (period == null ? "THIS_MONTH" : period.toUpperCase()) {
            case "7_DAYS"     -> new LocalDate[]{ today.minusDays(6), today };
            case "30_DAYS"    -> new LocalDate[]{ today.minusDays(29), today };
            case "3_MONTHS"   -> new LocalDate[]{ today.minusMonths(3).withDayOfMonth(1), today };
            case "6_MONTHS"   -> new LocalDate[]{ today.minusMonths(6).withDayOfMonth(1), today };
            case "1_YEAR"     -> new LocalDate[]{ today.minusYears(1).withDayOfMonth(1), today };
            case "LAST_MONTH" -> {
                YearMonth ym = YearMonth.from(today).minusMonths(1);
                yield new LocalDate[]{ ym.atDay(1), ym.atEndOfMonth() };
            }
            case "YTD"        -> new LocalDate[]{ today.withDayOfYear(1), today };
            default           -> { // THIS_MONTH
                YearMonth ym = YearMonth.from(today);
                yield new LocalDate[]{ ym.atDay(1), ym.atEndOfMonth() };
            }
        };
    }

    private void validateCustomRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("The 'from' date must not be after the 'to' date.");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366 * 5) {
            throw new BadRequestException("The selected range is too large. Choose a span of 5 years or less.");
        }
    }
}
