package com.finapse.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CardAnalyticsResponse(
        UUID cardId,
        String cardName,
        BigDecimal totalSpending,
        BigDecimal totalCashback,
        BigDecimal totalPayments,
        BigDecimal outstanding,
        BigDecimal availableCredit,
        int transactionCount
) {}
