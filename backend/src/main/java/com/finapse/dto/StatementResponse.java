package com.finapse.dto;

import com.finapse.entity.Statement;
import com.finapse.enums.ImportStatus;
import com.finapse.enums.StatementType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StatementResponse(
        UUID id,
        StatementType statementType,
        String originalFileName,
        String accountId,
        String accountName,
        String cardId,
        String cardName,
        int transactionCount,
        ImportStatus importStatus,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDateTime uploadedAt,
        LocalDateTime processedAt
) {
    public static StatementResponse from(Statement s) {
        return new StatementResponse(
                s.getId(),
                s.getStatementType(),
                s.getOriginalFileName(),
                s.getAccount() != null ? s.getAccount().getId().toString() : null,
                s.getAccount() != null ? s.getAccount().getName() : null,
                s.getCard() != null ? s.getCard().getId().toString() : null,
                s.getCard() != null ? s.getCard().getName() : null,
                s.getTransactionCount(),
                s.getImportStatus(),
                s.getPeriodStart(),
                s.getPeriodEnd(),
                s.getUploadedAt(),
                s.getProcessedAt()
        );
    }
}
