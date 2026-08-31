package com.finapse.dto;

import com.finapse.enums.BudgetPeriod;
import com.finapse.enums.BudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        BigDecimal limitAmount,
        BudgetPeriod period,
        int alertThreshold,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal spent,
        BigDecimal remaining,
        double percentUsed,
        BudgetStatus status,
        /** Straight-line projection of period-end spend based on elapsed days. */
        BigDecimal projectedSpend,
        boolean projectedToExceed
) {}
