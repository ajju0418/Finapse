package com.finapse.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CardCreateRequest(

        @NotBlank(message = "Card name is required")
        @Size(max = 150)
        String name,

        @Size(max = 100)
        String issuer,

        @Pattern(regexp = "\\d{4}", message = "Last four digits must be exactly 4 digits")
        String lastFourDigits,

        @DecimalMin(value = "0.00", message = "Credit limit must be positive")
        @Digits(integer = 13, fraction = 2)
        BigDecimal creditLimit,

        @Min(1) @Max(31)
        Integer billingCycleDay,

        @Min(1) @Max(31)
        Integer paymentDueDay
) {}
