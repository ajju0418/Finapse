package com.finapse.entity;

import com.finapse.enums.TransactionLinkStatus;
import com.finapse.enums.TransactionLinkType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_links")
@Getter @Setter
public class TransactionLink {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private UUID id;

    // source and target must differ — enforced by DB CHECK constraint
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_transaction_id", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Transaction sourceTransaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_transaction_id", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Transaction targetTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 30)
    private TransactionLinkType linkType;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionLinkStatus status = TransactionLinkStatus.SUGGESTED;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
