package com.finapse.service;

import com.finapse.dto.AuthResponse;
import com.finapse.dto.LoginRequest;
import com.finapse.dto.RegisterRequest;
import com.finapse.dto.UserResponse;
import com.finapse.entity.User;
import com.finapse.enums.Role;
import com.finapse.exception.ConflictException;
import com.finapse.exception.UnauthorizedException;
import com.finapse.repository.UserRepository;
import com.finapse.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Orchestrates registration, sign-in, token refresh and sign-out.
 *
 * <p>The result of every flow is an {@link AuthResult}: a short-lived access JWT for
 * the client to hold in memory, and an opaque refresh handle the controller turns
 * into an httpOnly cookie.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String GENERIC_CREDENTIALS_ERROR = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public record AuthResult(AuthResponse body, String refreshToken) {}

    @Transactional
    public AuthResult register(RegisterRequest request, String userAgent) {
        String email = normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }

        User user = userRepository.findUnclaimedLegacyUser().orElseGet(User::new);
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setActive(true);
        user.setLastLoginAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("Registered new account {}", saved.getId());

        return buildResult(saved, userAgent);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String clientIp, String userAgent) {
        String email = normalize(request.email());

        loginAttemptService.assertNotLocked(email, clientIp);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (DisabledException ex) {
            loginAttemptService.recordFailure(email, clientIp);
            throw new UnauthorizedException("This account has been deactivated.");
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(email, clientIp);
            // Identical message whether the email is unknown or the password is wrong,
            // so the endpoint cannot be used to discover registered addresses.
            throw new UnauthorizedException(GENERIC_CREDENTIALS_ERROR);
        }

        loginAttemptService.recordSuccess(email, clientIp);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(GENERIC_CREDENTIALS_ERROR));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildResult(user, userAgent);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("No active session.");
        }

        RefreshTokenService.IssuedToken rotated = refreshTokenService.rotate(rawRefreshToken, userAgent);
        User user = rotated.entity().getUser();

        AuthResponse body = AuthResponse.of(
                jwtService.generateAccessToken(user),
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user));

        return new AuthResult(body, rotated.rawValue());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private AuthResult buildResult(User user, String userAgent) {
        RefreshTokenService.IssuedToken refresh = refreshTokenService.issue(user, userAgent);
        AuthResponse body = AuthResponse.of(
                jwtService.generateAccessToken(user),
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user));
        return new AuthResult(body, refresh.rawValue());
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
