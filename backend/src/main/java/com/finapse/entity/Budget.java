package com.finapse.entity;

import com.finapse.enums.BudgetPeriod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A spending cap the user sets for a category (or overall, when category is null)
 * over a repeating period. Budgets never alter transaction data — they are a pure
 * read-side comparison against actual spending.
 */
@Entity
@Table(name = "budgets")
@Getter @Setter
public class Budget {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private User user;

    /** Null means this is the overall spending budget for the period. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Category category;

    @Column(name = "limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    /** Percentage of the limit at which the budget is flagged AT_RISK. */
    @Column(name = "alert_threshold", nullable = false)
    private int alertThreshold = 80;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
