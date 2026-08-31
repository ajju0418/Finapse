package com.finapse.dto;

import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A transaction the user enters by hand — typically cash spending. */
public record ManualTransactionRequest(
        @NotNull(message = "A transaction date is required.")
        LocalDate transactionDate,

        @NotBlank(message = "A description is required.")
        @Size(max = 500, message = "The description must be 500 characters or fewer.")
        String description,

        @NotNull(message = "An amount is required.")
        @DecimalMin(value = "0.01", message = "The amount must be greater than zero.")
        BigDecimal amount,

        @NotNull(message = "Specify whether money went out (DEBIT) or came in (CREDIT).")
        TransactionDirection direction,

        @NotNull(message = "A transaction type is required.")
        TransactionType transactionType,

        UUID categoryId,

        /** Exactly one of accountId or cardId must be supplied. */
        UUID accountId,
        UUID cardId
) {}
