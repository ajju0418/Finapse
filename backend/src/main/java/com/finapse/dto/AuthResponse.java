package com.finapse.dto;

/**
 * Result of a successful authentication.
 *
 * <p>The refresh token is never part of this body — it is delivered as an
 * httpOnly cookie so that JavaScript (and therefore any XSS payload) cannot read it.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String accessToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
