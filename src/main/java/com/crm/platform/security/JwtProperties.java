package com.crm.platform.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtProperties {

    public static final String DEFAULT_ISSUER = "cs-crm-2026";
    public static final long DEFAULT_EXPIRATION_MS = 3600000L; // 1 hour (3,600,000 ms)
    public static final int MIN_SECRET_BYTES = 32;

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.issuer:" + DEFAULT_ISSUER + "}")
    private String issuer = DEFAULT_ISSUER;

    @Value("${security.jwt.expiration-ms:" + DEFAULT_EXPIRATION_MS + "}")
    private long expirationMs = DEFAULT_EXPIRATION_MS;

    public JwtProperties() {
    }

    public JwtProperties(String secret) {
        this(secret, DEFAULT_ISSUER, DEFAULT_EXPIRATION_MS);
    }

    public JwtProperties(String secret, String issuer, long expirationMs) {
        this.secret = secret;
        this.issuer = issuer;
        this.expirationMs = expirationMs;
        validate();
    }

    @PostConstruct
    public void validate() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory and must not be blank. Please set the JWT_SECRET environment variable.");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(String.format(
                    "JWT secret must be at least %d UTF-8 bytes (256 bits) for HS256, but was %d bytes",
                    MIN_SECRET_BYTES, secretBytes.length));
        }
        if (issuer == null || issuer.trim().isEmpty()) {
            throw new IllegalStateException("JWT issuer must not be empty");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration-ms must be positive");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
