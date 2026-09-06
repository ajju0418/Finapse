package com.finapse.dto;

import com.finapse.entity.Account;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        String institutionName,
        String accountType,
        String lastFourDigits,
        String currency,
        boolean hasStatementPassword,
        boolean isActive,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getName(),
                a.getInstitutionName(),
                a.getAccountType().name(),
                a.getLastFourDigits(),
                a.getCurrency(),
                a.getStatementPassword() != null && !a.getStatementPassword().isBlank(),
                a.isActive(),
                a.getCreatedAt()
        );
    }
}
