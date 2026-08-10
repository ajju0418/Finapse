package com.finapse.dto;

import java.math.BigDecimal;

public record CategorySpendingDto(
        String categoryName,
        BigDecimal amount,
        double percentage
) {}
