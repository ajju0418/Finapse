package com.finapse.entity;

import com.finapse.enums.ImportStatus;
import com.finapse.enums.StatementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "statements")
@Getter @Setter
public class Statement {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Exactly one of account or card must be set — enforced by DB CHECK constraint
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 20)
    private StatementType statementType;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 20)
    private ImportStatus importStatus = ImportStatus.UPLOADED;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
