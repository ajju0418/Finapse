package com.finapse.enums;

public enum BudgetStatus {
    /** Under 80% of the limit. */
    ON_TRACK,
    /** Between 80% and 100% of the limit. */
    AT_RISK,
    /** Over the limit. */
    EXCEEDED
}
