package com.finapse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardAnalyticsResponse(
        UUID cardId,
        String cardName,
        BigDecimal totalSpending,
        BigDecimal totalCashback,
        BigDecimal totalPayments,
        BigDecimal outstanding,
        BigDecimal availableCredit,
        int transactionCount,
        // --- Billing cycle & utilisation ---
        BigDecimal creditLimit,
        /** Outstanding as a percentage of the credit limit; null when no limit is set. */
        Double utilizationPercent,
        /** LOW below 30%, MODERATE 30-70%, HIGH above 70%; null when no limit. */
        String utilizationBand,
        LocalDate currentCycleStart,
        LocalDate currentCycleEnd,
        BigDecimal currentCycleSpend,
        LocalDate nextStatementDate,
        LocalDate nextDueDate,
        Integer daysUntilDue
) {}
