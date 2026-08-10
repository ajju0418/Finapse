package com.finapse.entity;

import com.finapse.enums.ReviewStatus;
import com.finapse.enums.ReviewType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_reviews")
@Getter @Setter
public class ReconciliationReview {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_link_id", nullable = false)
    private TransactionLink transactionLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 30)
    private ReviewType reviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "system_reason", length = 1000)
    private String systemReason;

    @Column(name = "user_decision", length = 100)
    private String userDecision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
