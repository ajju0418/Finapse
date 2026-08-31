package com.finapse.service;

import com.finapse.config.AuthProperties;
import com.finapse.exception.TooManyAttemptsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force throttle keyed by email plus client IP.
 *
 * <p>Counting both dimensions means one attacker cannot lock a victim out of their
 * own account by spamming failures from elsewhere, while still stopping password
 * spraying from a single host.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private record Attempt(int count, Instant lockedUntil) {}

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final AuthProperties props;

    public void assertNotLocked(String email, String clientIp) {
        Instant now = Instant.now();
        for (String key : keys(email, clientIp)) {
            Attempt attempt = attempts.get(key);
            if (attempt != null && attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(now)) {
                throw new TooManyAttemptsException(
                        "Too many failed sign-in attempts. Please try again later.",
                        Duration.between(now, attempt.lockedUntil()));
            }
        }
    }

    public void recordFailure(String email, String clientIp) {
        Instant now = Instant.now();
        for (String key : keys(email, clientIp)) {
            attempts.compute(key, (k, existing) -> {
                int count = (existing == null || isStale(existing, now)) ? 1 : existing.count() + 1;
                Instant lockedUntil = count >= props.getMaxLoginAttempts()
                        ? now.plus(props.getLoginLockoutDuration())
                        : null;
                return new Attempt(count, lockedUntil);
            });
        }
    }

    public void recordSuccess(String email, String clientIp) {
        for (String key : keys(email, clientIp)) {
            attempts.remove(key);
        }
    }

    private boolean isStale(Attempt attempt, Instant now) {
        return attempt.lockedUntil() != null && attempt.lockedUntil().isBefore(now);
    }

    private String[] keys(String email, String clientIp) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return new String[] { "email:" + normalizedEmail, "ip:" + clientIp };
    }

    /** Keeps the map from growing without bound on a long-running process. */
    @Scheduled(fixedDelay = 15 * 60 * 1000L)
    void evictExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(e ->
                e.getValue().lockedUntil() != null && e.getValue().lockedUntil().isBefore(now));
    }
}
