package com.finapse.security;

import com.finapse.config.AuthProperties;
import com.finapse.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies short-lived stateless access tokens (HS256).
 *
 * <p>Only access tokens are JWTs. Refresh tokens are opaque random handles stored
 * server-side, which keeps revocation immediate and reliable.
 */
@Service
@Slf4j
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";

    private final SecretKey signingKey;
    private final AuthProperties props;

    public JwtService(AuthProperties props) {
        this.props = props;
        this.signingKey = resolveSigningKey(props.getJwtSecret());
    }

    private static SecretKey resolveSigningKey(String configured) {
        if (configured == null || configured.isBlank()) {
            log.warn("""
                    ------------------------------------------------------------------
                    finapse.auth.jwt-secret is not set. An ephemeral signing key has
                    been generated for this process, so every restart invalidates all
                    issued access tokens. Set FINAPSE_JWT_SECRET (Base64, >= 32 bytes)
                    before deploying anywhere other than a local machine.
                    ------------------------------------------------------------------""");
            return Jwts.SIG.HS256.key().build();
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(configured);
        } catch (IllegalArgumentException ex) {
            // Allow a raw (non Base64) secret so misconfiguration fails loudly, not silently.
            keyBytes = configured.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "finapse.auth.jwt-secret must decode to at least 32 bytes (256 bits) for HS256.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(props.getAccessTokenTtl());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getIssuer())
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getName())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return props.getAccessTokenTtl().toSeconds();
    }

    /**
     * Verifies signature, expiry, issuer and token type.
     *
     * @return the parsed claims, or empty when the token is unusable for any reason.
     */
    public Optional<Claims> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(props.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            // Expected for expired/forged tokens — never leak details to the caller.
            log.debug("Rejected access token: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<UUID> extractUserId(Claims claims) {
        try {
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
