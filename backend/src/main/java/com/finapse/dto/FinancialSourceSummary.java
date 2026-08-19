package com.finapse.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialSourceSummary(
        UUID id,
        String name,
        String institution, // issuer for cards, institutionName for accounts
        BigDecimal currentBalance, // outstanding for cards, netChange for accounts
        BigDecimal totalSpending, // spending for cards, outflow for accounts
        boolean isCard
) {}
