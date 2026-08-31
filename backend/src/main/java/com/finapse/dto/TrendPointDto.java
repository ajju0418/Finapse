package com.finapse.dto;

import java.math.BigDecimal;

/** One bucket in the cash-flow trend series. */
public record TrendPointDto(
        /** ISO month (yyyy-MM) or ISO date for daily buckets. */
        String bucket,
        String label,
        BigDecimal income,
        BigDecimal grossExpenses,
        BigDecimal refunds,
        BigDecimal actualSpending,
        BigDecimal cashback,
        BigDecimal netCashFlow,
        int transactionCount
) {}
