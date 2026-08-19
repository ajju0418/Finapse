package com.finapse.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountAnalyticsResponse(
        UUID accountId,
        String accountName,
        BigDecimal totalInflow,
        BigDecimal totalOutflow,
        BigDecimal netChange,
        int transactionCount
) {}
