package com.finapse.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents one row extracted from a CSV file before any business classification.
 * Carries the original source row number for full traceability.
 */
public record RawTransactionRecord(
        int sourceRowNumber,
        LocalDate transactionDate,
        LocalDate postedDate,        // nullable
        String description,
        BigDecimal amount,
        com.finapse.enums.TransactionDirection direction,
        String rawDebit,             // original cell value, kept for audit
        String rawCredit,            // original cell value, kept for audit
        String rawDate               // original cell value, kept for audit
) {}
