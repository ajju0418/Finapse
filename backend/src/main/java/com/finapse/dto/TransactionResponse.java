package com.finapse.dto;

import com.finapse.entity.Transaction;
import com.finapse.enums.ClassificationSource;
import com.finapse.enums.ReconciliationStatus;
import com.finapse.enums.TransactionDirection;
import com.finapse.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID statementId,
        UUID accountId,
        UUID cardId,
        UUID merchantId,
        UUID categoryId,
        String merchantName,
        String categoryName,
        LocalDate transactionDate,
        LocalDate postedDate,
        String description,
        BigDecimal amount,
        TransactionDirection direction,
        TransactionType transactionType,
        BigDecimal cashbackAmount,
        ReconciliationStatus reconciliationStatus,
        Integer sourceRowNumber,
        // --- Explainability: why the engine decided what it decided ---
        ClassificationSource classificationSource,
        Double classificationConfidence,
        String classificationReason,
        // --- Recurring / subscription detection ---
        Boolean isRecurring,
        String recurringGroupId,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getStatement().getId(),
                tx.getAccount() != null ? tx.getAccount().getId() : null,
                tx.getCard() != null ? tx.getCard().getId() : null,
                tx.getMerchant() != null ? tx.getMerchant().getId() : null,
                tx.getCategory() != null ? tx.getCategory().getId() : null,
                tx.getMerchant() != null ? tx.getMerchant().getName() : null,
                tx.getCategory() != null ? tx.getCategory().getName() : null,
                tx.getTransactionDate(),
                tx.getPostedDate(),
                tx.getDescription(),
                tx.getAmount(),
                tx.getDirection(),
                tx.getTransactionType(),
                tx.getCashbackAmount(),
                tx.getReconciliationStatus(),
                tx.getSourceRowNumber(),
                tx.getClassificationSource(),
                tx.getClassificationConfidence(),
                tx.getClassificationReason(),
                tx.getIsRecurring() != null && tx.getIsRecurring(),
                tx.getRecurringGroupId(),
                tx.getCreatedAt()
        );
    }
}
