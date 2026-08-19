package com.finapse.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal income,
        BigDecimal grossExpenses,
        BigDecimal refunds,
        BigDecimal actualSpending,
        BigDecimal cashback,
        BigDecimal netCashFlow,
        String periodStart,
        String periodEnd,
        List<CategorySpendingDto> categoryBreakdown,
        List<MerchantSpendingDto> topMerchants,
        List<TransactionResponse> recentTransactions,
        long pendingReviewCount,
        List<FinancialSourceSummary> sourceSummaries
) {}
