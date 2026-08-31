package com.finapse.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public final class CookieUtil {

    private CookieUtil() {}

    /**
     * Builds the refresh-token cookie.
     *
     * <p>{@code httpOnly} keeps the value out of reach of any script on the page, so an
     * XSS bug cannot steal a long-lived session. The path is {@code /} rather than the
     * narrower {@code /api/auth} because the frontend's route guard needs to see that a
     * session cookie exists before rendering protected pages; the value itself stays
     * unreadable either way.
     */
    public static ResponseCookie build(String name, String value, Duration maxAge,
                                       boolean secure, String sameSite) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie expired(String name, boolean secure, String sameSite) {
        return build(name, "", Duration.ZERO, secure, sameSite);
    }

    public static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    public static HttpHeaders asHeader(ResponseCookie cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }
}
