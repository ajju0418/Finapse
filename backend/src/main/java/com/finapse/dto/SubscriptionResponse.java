package com.finapse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A detected recurring charge, aggregated from every transaction sharing a
 * {@code recurringGroupId}.
 */
public record SubscriptionResponse(
        String recurringGroupId,
        String merchantName,
        UUID merchantId,
        String categoryName,
        /** Most recent charge amount. */
        BigDecimal amount,
        /** Average across all occurrences — differs from {@code amount} when the price changed. */
        BigDecimal averageAmount,
        int occurrences,
        /** Mean days between charges; ~30 for monthly, ~365 for annual. */
        long averageIntervalDays,
        String cadence,
        LocalDate firstCharge,
        LocalDate lastCharge,
        /** Projected next charge, extrapolated from the average interval. */
        LocalDate estimatedNextCharge,
        /** Total spent on this subscription across the window. */
        BigDecimal totalSpent,
        /** Latest amount is materially higher than the previous one. */
        boolean priceIncreased,
        /** No charge for well over the expected interval — possibly cancelled. */
        boolean possiblyInactive
) {}
