package com.finapse.dto;

import com.finapse.entity.Card;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String name,
        String issuer,
        String lastFourDigits,
        BigDecimal creditLimit,
        Integer billingCycleDay,
        Integer paymentDueDay,
        boolean isActive,
        LocalDateTime createdAt
) {
    public static CardResponse from(Card c) {
        return new CardResponse(
                c.getId(),
                c.getName(),
                c.getIssuer(),
                c.getLastFourDigits(),
                c.getCreditLimit(),
                c.getBillingCycleDay(),
                c.getPaymentDueDay(),
                c.isActive(),
                c.getCreatedAt()
        );
    }
}
