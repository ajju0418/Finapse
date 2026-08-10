package com.finapse.dto;

import jakarta.validation.constraints.NotNull;

public record ReconciliationDecisionRequest(
        @NotNull Boolean approved,
        String note
) {}
