package com.finapse.dto;

import com.finapse.enums.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A user correcting how a transaction was classified.
 *
 * <p>When {@code applyToSimilar} is true the correction is also stored as a
 * {@code UserClassificationRule}, so future imports with the same normalized
 * narration are classified this way automatically.
 */
public record TransactionCorrectionRequest(

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        /** Optional — null leaves the existing category untouched. */
        UUID categoryId,

        /** Teach the engine from this correction. Defaults to true. */
        Boolean applyToSimilar
) {
    public boolean shouldLearn() {
        return applyToSimilar == null || applyToSimilar;
    }
}
