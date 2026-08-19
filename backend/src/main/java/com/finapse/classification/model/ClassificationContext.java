package com.finapse.classification.model;

import com.finapse.entity.Merchant;
import com.finapse.entity.Transaction;
import com.finapse.enums.EntityType;
import com.finapse.enums.TransactionDirection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class ClassificationContext {

    private Transaction transaction;
    private String rawNarration;
    private String normalizedNarration;
    private BigDecimal amount;
    private TransactionDirection direction;
    private Merchant merchant;
    private EntityType entityType;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
