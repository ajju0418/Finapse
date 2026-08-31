package com.finapse.controller;

import com.finapse.config.AuthProperties;
import com.finapse.dto.AuthResponse;
import com.finapse.dto.ChangePasswordRequest;
import com.finapse.dto.LoginRequest;
import com.finapse.dto.RegisterRequest;
import com.finapse.dto.UserResponse;
import com.finapse.service.AuthService;
import com.finapse.service.UserService;
import com.finapse.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthProperties authProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.register(request, userAgent(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(refreshCookieHeader(result.refreshToken()))
                .body(result.body());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.login(request, clientIp(httpRequest), userAgent(httpRequest));
        return ResponseEntity.ok()
                .headers(refreshCookieHeader(result.refreshToken()))
                .body(result.body());
    }

    /** Exchanges the httpOnly refresh cookie for a new access token, rotating the cookie. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest) {
        String rawToken = CookieUtil.read(httpRequest, authProperties.getRefreshCookieName()).orElse(null);
        AuthService.AuthResult result = authService.refresh(rawToken, userAgent(httpRequest));
        return ResponseEntity.ok()
                .headers(refreshCookieHeader(result.refreshToken()))
                .body(result.body());
    }

    /** Always succeeds so a stale client can still clear its cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        CookieUtil.read(httpRequest, authProperties.getRefreshCookieName())
                .ifPresent(authService::logout);

        ResponseCookie cleared = CookieUtil.expired(
                authProperties.getRefreshCookieName(),
                authProperties.isCookieSecure(),
                authProperties.getCookieSameSite());

        return ResponseEntity.noContent()
                .headers(CookieUtil.asHeader(cleared))
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(UserResponse.from(userService.getCurrentUser()));
    }

    /**
     * Changes the password and rotates the session. Every other refresh token for
     * this account is revoked, so other devices must sign in again.
     */
    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                       HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.changePassword(
                userService.getCurrentUser(), request, userAgent(httpRequest));
        return ResponseEntity.ok()
                .headers(refreshCookieHeader(result.refreshToken()))
                .body(result.body());
    }

    private HttpHeaders refreshCookieHeader(String rawToken) {
        return CookieUtil.asHeader(CookieUtil.build(
                authProperties.getRefreshCookieName(),
                rawToken,
                authProperties.getRefreshTokenTtl(),
                authProperties.isCookieSecure(),
                authProperties.getCookieSameSite()));
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
