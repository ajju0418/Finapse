package com.finapse.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Strongly-typed binding for the {@code finapse.auth.*} configuration block.
 */
@ConfigurationProperties(prefix = "finapse.auth")
@Validated
@Getter @Setter
public class AuthProperties {

    /**
     * Base64-encoded HMAC signing key, minimum 256 bits.
     * Left empty only for local development, where an ephemeral key is generated.
     */
    private String jwtSecret = "";

    @NotBlank
    private String issuer = "finapse";

    @NotNull
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    @NotNull
    private Duration refreshTokenTtl = Duration.ofDays(14);

    @NotBlank
    private String refreshCookieName = "finapse_refresh";

    private boolean cookieSecure = false;

    @NotBlank
    private String cookieSameSite = "Lax";

    @Min(1)
    private int maxLoginAttempts = 5;

    @NotNull
    private Duration loginLockoutDuration = Duration.ofMinutes(15);

    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
