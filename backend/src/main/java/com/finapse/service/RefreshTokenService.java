package com.finapse.service;

import com.finapse.config.AuthProperties;
import com.finapse.entity.RefreshToken;
import com.finapse.entity.User;
import com.finapse.exception.UnauthorizedException;
import com.finapse.repository.RefreshTokenRepository;
import com.finapse.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues, rotates and revokes opaque refresh tokens.
 *
 * <p>Tokens are 256 bits of {@link SecureRandom} entropy, stored only as a SHA-256
 * digest. Every successful refresh rotates the token; presenting an already-rotated
 * token revokes the entire session family, which contains the damage from a stolen
 * cookie to a single rotation window.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties props;

    /** A freshly minted token plus the raw value that must be sent to the browser. */
    public record IssuedToken(String rawValue, RefreshToken entity) {}

    @Transactional
    public IssuedToken issue(User user, String userAgent) {
        return persist(user, UUID.randomUUID(), userAgent);
    }

    /**
     * Validates the presented token and swaps it for a new one.
     *
     * @throws UnauthorizedException when the token is unknown, expired, or replayed.
     */
    @Transactional
    public IssuedToken rotate(String rawToken, String userAgent) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(HashUtil.sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Session expired. Please sign in again."));

        if (existing.isRevoked()) {
            // A revoked token being replayed means the cookie leaked. Burn the family.
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), Instant.now());
            log.warn("Refresh token reuse detected for user {} — revoked session family {}",
                    existing.getUser().getId(), existing.getFamilyId());
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }

        if (existing.isExpired()) {
            throw new UnauthorizedException("Session expired. Please sign in again.");
        }

        User user = existing.getUser();
        if (!user.isActive()) {
            refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
            throw new UnauthorizedException("This account has been deactivated.");
        }

        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);

        return persist(user, existing.getFamilyId(), userAgent);
    }

    /** Revokes the single session behind this token. Silent when already gone. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(HashUtil.sha256(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    /** Signs the user out everywhere — used on password change or deactivation. */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private IssuedToken persist(User user, UUID familyId, String userAgent) {
        byte[] entropy = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(entropy);
        String rawValue = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(HashUtil.sha256(rawValue));
        token.setFamilyId(familyId);
        token.setExpiresAt(Instant.now().plus(props.getRefreshTokenTtl()));
        token.setUserAgent(truncate(userAgent));

        return new IssuedToken(rawValue, refreshTokenRepository.save(token));
    }

    private String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255);
    }

    /** Daily sweep of tokens that can no longer be used. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        int removed = refreshTokenRepository.deleteExpiredBefore(Instant.now().minus(props.getRefreshTokenTtl()));
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }
}
