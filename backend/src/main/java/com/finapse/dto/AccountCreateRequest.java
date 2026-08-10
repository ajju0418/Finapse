package com.finapse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(

        @NotBlank(message = "Account name is required")
        @Size(max = 150)
        String name,

        @Size(max = 150)
        String institutionName,

        @Pattern(regexp = "\\d{4}", message = "Last four digits must be exactly 4 digits")
        String lastFourDigits,

        @Size(min = 3, max = 3)
        String currency
) {}
