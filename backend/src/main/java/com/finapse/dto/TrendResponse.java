package com.finapse.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrendResponse(
        String periodStart,
        String periodEnd,
        String granularity,
        List<TrendPointDto> points,
        /** Simple linear comparison of the latest bucket against the previous one. */
        BigDecimal spendingChangeAmount,
        Double spendingChangePercent,
        BigDecimal averageMonthlySpend,
        BigDecimal averageMonthlyIncome
) {}
