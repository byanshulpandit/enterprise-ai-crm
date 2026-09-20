package com.crm.platform.security;

import com.crm.platform.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;
    private final JwtParser jwtParser;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(this.signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build();
    }

    public String generateToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null when generating token");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("User canonical username must not be null or empty");
        }
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getExpirationMs());
        return generateToken(user, now, expiry);
    }

    public String generateToken(User user, Instant issuedAt, Instant expiresAt) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null when generating token");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("User canonical username must not be null or empty");
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Issued at and expiration timestamps must not be null");
        }

        return Jwts.builder()
                .header()
                    .type("JWT")
                .and()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole() != null ? user.getRole().name() : null)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .signWith(this.signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token must not be null or empty");
        }
        return jwtParser.parseSignedClaims(token.trim()).getPayload();
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        Object uid = claims.get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return claims.get("uid", Long.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
}
