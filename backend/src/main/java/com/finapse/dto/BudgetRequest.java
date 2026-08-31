package com.finapse.dto;

import com.finapse.enums.BudgetPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequest(
        /** Null creates/updates the overall spending budget. */
        UUID categoryId,

        @NotNull(message = "A budget limit is required.")
        @DecimalMin(value = "0.01", message = "The budget limit must be greater than zero.")
        BigDecimal limitAmount,

        BudgetPeriod period,

        @Min(value = 1, message = "The alert threshold must be between 1 and 100.")
        @Max(value = 100, message = "The alert threshold must be between 1 and 100.")
        Integer alertThreshold
) {
    public BudgetPeriod periodOrDefault() {
        return period != null ? period : BudgetPeriod.MONTHLY;
    }

    public int alertThresholdOrDefault() {
        return alertThreshold != null ? alertThreshold : 80;
    }
}
