package com.finapse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A single issued refresh token.
 *
 * <p>Only the SHA-256 hash of the token is persisted, so a database leak does not
 * hand out usable sessions. Tokens are rotated on every use: the presented token is
 * revoked and a replacement is issued within the same {@code familyId}. If a token
 * that was already revoked is presented again, the whole family is revoked because
 * that signals the token was stolen and replayed.
 */
@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uq_refresh_tokens_hash", columnNames = "token_hash"),
        indexes = {
                @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_family", columnList = "family_id")
        }
)
@Getter @Setter
public class RefreshToken {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private User user;

    /** Hex-encoded SHA-256 of the opaque token handed to the browser. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** Groups every rotation of one login session. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "family_id", columnDefinition = "CHAR(36)", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
