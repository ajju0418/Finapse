package com.finapse.dto;

import java.math.BigDecimal;

public record MerchantSpendingDto(
        String merchantName,
        BigDecimal amount,
        int transactionCount
) {}
