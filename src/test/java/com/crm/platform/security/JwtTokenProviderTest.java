package com.crm.platform.security;

import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtTokenProviderTest {

    private static final String TEST_SECRET = "deterministic-test-jwt-secret-key-at-least-32-bytes-long-2026";
    private static final String TEST_ISSUER = "cs-crm-2026";
    private static final long TEST_EXPIRATION_MS = 3600000L;

    private JwtProperties jwtProperties;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(TEST_SECRET, TEST_ISSUER, TEST_EXPIRATION_MS);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    private User createSampleUser(Long id, String username, RoleEnum role) {
        User user = new User(username, username + "@example.com", "$2a$12$sampleHashedPassword12345", role);
        user.setId(id);
        return user;
    }

    @Test
    @DisplayName("1. Valid token generation produces a non-null compact JWT")
    void testValidTokenGeneration() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        assertThat(token).isNotBlank();
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("2. sub claim is canonical username")
    void testSubIsCanonicalUsername() {
        User user = createSampleUser(42L, "canonical_john", RoleEnum.ROLE_MARKETER);
        String token = jwtTokenProvider.generateToken(user);

        String username = jwtTokenProvider.extractUsername(token);
        assertThat(username).isEqualTo("canonical_john");

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("canonical_john");
    }

    @Test
    @DisplayName("3. uid claim is correct user id")
    void testUidClaimIsCorrect() {
        User user = createSampleUser(105L, "test_marketer", RoleEnum.ROLE_MARKETER);
        String token = jwtTokenProvider.generateToken(user);

        Long uid = jwtTokenProvider.extractUserId(token);
        assertThat(uid).isEqualTo(105L);
    }

    @Test
    @DisplayName("4. role claim is correct canonical role string")
    void testRoleClaimIsCorrect() {
        User admin = createSampleUser(1L, "admin1", RoleEnum.ROLE_ADMIN);
        String adminToken = jwtTokenProvider.generateToken(admin);
        assertThat(jwtTokenProvider.extractRole(adminToken)).isEqualTo("ROLE_ADMIN");

        User marketer = createSampleUser(2L, "marketer1", RoleEnum.ROLE_MARKETER);
        String marketerToken = jwtTokenProvider.generateToken(marketer);
        assertThat(jwtTokenProvider.extractRole(marketerToken)).isEqualTo("ROLE_MARKETER");
    }

    @Test
    @DisplayName("5. issuer claim is correct (cs-crm-2026)")
    void testIssuerClaimIsCorrect() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getIssuer()).isEqualTo("cs-crm-2026");
    }

    @Test
    @DisplayName("6. iat exists and is near issuance timestamp")
    void testIatExists() {
        Instant before = Instant.now().minusSeconds(2);
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        String token = jwtTokenProvider.generateToken(user);
        Instant after = Instant.now().plusSeconds(2);

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getIssuedAt()).isNotNull();

        Instant iat = claims.getIssuedAt().toInstant();
        assertThat(iat).isBetween(before, after);
    }

    @Test
    @DisplayName("7. exp exists and is after iat")
    void testExpExists() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("8. jti exists and is a valid UUID")
    void testJtiExistsAndIsUuidLike() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        Claims claims = jwtTokenProvider.parseClaims(token);
        String jti = claims.getId();
        assertThat(jti).isNotBlank();

        assertThatCode(() -> UUID.fromString(jti))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("9. Token is accepted before expiration")
    void testTokenAcceptedBeforeExpiration() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        boolean valid = jwtTokenProvider.isTokenValid(token);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("10. Malformed token is rejected")
    void testMalformedTokenRejected() {
        assertThat(jwtTokenProvider.isTokenValid("not.a.valid.jwt")).isFalse();
        assertThat(jwtTokenProvider.isTokenValid("malformed-token-string")).isFalse();
        assertThat(jwtTokenProvider.isTokenValid("")).isFalse();
        assertThat(jwtTokenProvider.isTokenValid("   ")).isFalse();
        assertThat(jwtTokenProvider.isTokenValid(null)).isFalse();

        assertThatThrownBy(() -> jwtTokenProvider.parseClaims("not.a.valid.jwt"))
                .isInstanceOf(MalformedJwtException.class);

        assertThatThrownBy(() -> jwtTokenProvider.parseClaims(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> jwtTokenProvider.parseClaims(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("11. Invalid signature is rejected")
    void testInvalidSignatureRejected() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "different-secret-key-at-least-32-bytes-long-for-test!!".getBytes(StandardCharsets.UTF_8));

        String foreignToken = Jwts.builder()
                .header().type("JWT").and()
                .subject("attacker")
                .issuer(TEST_ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .id(UUID.randomUUID().toString())
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtTokenProvider.isTokenValid(foreignToken)).isFalse();

        assertThatThrownBy(() -> jwtTokenProvider.parseClaims(foreignToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("12. Wrong issuer is rejected")
    void testWrongIssuerRejected() {
        SecretKey validKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

        String foreignIssuerToken = Jwts.builder()
                .header().type("JWT").and()
                .subject("admin_user")
                .issuer("malicious-issuer")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .id(UUID.randomUUID().toString())
                .signWith(validKey, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtTokenProvider.isTokenValid(foreignIssuerToken)).isFalse();

        assertThatThrownBy(() -> jwtTokenProvider.parseClaims(foreignIssuerToken))
                .isInstanceOf(IncorrectClaimException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    @DisplayName("13. Expired token is rejected")
    void testExpiredTokenRejected() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        Instant pastIat = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant pastExp = Instant.now().minus(1, ChronoUnit.HOURS);

        String expiredToken = jwtTokenProvider.generateToken(user, pastIat, pastExp);

        assertThat(jwtTokenProvider.isTokenValid(expiredToken)).isFalse();

        assertThatThrownBy(() -> jwtTokenProvider.parseClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("14. Two generated tokens have different jti")
    void testTwoGeneratedTokensHaveDifferentJti() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);

        String token1 = jwtTokenProvider.generateToken(user);
        String token2 = jwtTokenProvider.generateToken(user);

        String jti1 = jwtTokenProvider.parseClaims(token1).getId();
        String jti2 = jwtTokenProvider.parseClaims(token2).getId();

        assertThat(jti1).isNotBlank();
        assertThat(jti2).isNotBlank();
        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    @DisplayName("15. Secret missing or too short fails configuration validation")
    void testSecretMissingOrTooShortFailsValidation() {
        // Null secret
        assertThatThrownBy(() -> new JwtProperties(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory and must not be blank");

        // Empty secret
        assertThatThrownBy(() -> new JwtProperties(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory and must not be blank");

        // Blank secret
        assertThatThrownBy(() -> new JwtProperties("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory and must not be blank");

        // Secret shorter than 32 UTF-8 bytes (e.g. 16 bytes)
        assertThatThrownBy(() -> new JwtProperties("short-secret-16b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 UTF-8 bytes");

        // Secret exactly 31 bytes
        assertThatThrownBy(() -> new JwtProperties("1234567890123456789012345678901"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 UTF-8 bytes");

        // Secret exactly 32 bytes succeeds
        assertThatCode(() -> new JwtProperties("12345678901234567890123456789012"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("16. Token lifetime is approximately 1 hour (3,600,000 ms)")
    void testTokenLifetimeIsApproximatelyOneHour() {
        User user = createSampleUser(1L, "admin_user", RoleEnum.ROLE_ADMIN);
        String token = jwtTokenProvider.generateToken(user);

        Claims claims = jwtTokenProvider.parseClaims(token);
        Instant iat = claims.getIssuedAt().toInstant();
        Instant exp = claims.getExpiration().toInstant();

        Duration duration = Duration.between(iat, exp);
        assertThat(duration.toMillis()).isEqualTo(3600000L);
    }
}
