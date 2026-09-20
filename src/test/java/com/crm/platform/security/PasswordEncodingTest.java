package com.crm.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PasswordEncodingTest {

    private static final Pattern BCRYPT_COST_12_PATTERN = Pattern.compile("^\\$2[ab]\\$12\\$[./A-Za-z0-9]{53}$");

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Verify PasswordEncoder bean loads and is not null")
    void testPasswordEncoderBeanLoads() {
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    @DisplayName("Verify raw password is not equal to encoded value")
    void testRawPasswordNotEqualToEncoded() {
        String raw = "StrongP@ssw0rd123";
        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEqualTo(raw);
    }

    @Test
    @DisplayName("Verify passwordEncoder.matches(raw, encoded) evaluates to true")
    void testPasswordMatchesTrue() {
        String raw = "ValidPassword_2026";
        String encoded = passwordEncoder.encode(raw);

        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }

    @Test
    @DisplayName("Verify same raw password encoded twice produces different hashes due to salting")
    void testSaltingProducesDifferentHashes() {
        String raw = "MySecurePassword!";
        String hash1 = passwordEncoder.encode(raw);
        String hash2 = passwordEncoder.encode(raw);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches(raw, hash1)).isTrue();
        assertThat(passwordEncoder.matches(raw, hash2)).isTrue();
    }

    @Test
    @DisplayName("Verify wrong password does not match encoded hash")
    void testWrongPasswordDoesNotMatch() {
        String raw = "CorrectPassword123";
        String wrong = "IncorrectPassword123";
        String encoded = passwordEncoder.encode(raw);

        assertThat(passwordEncoder.matches(wrong, encoded)).isFalse();
    }

    @Test
    @DisplayName("Verify BCrypt strength is 12 via standard hash cost parameter")
    void testBcryptStrengthIs12() {
        String raw = "CheckStrength12#";
        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).matches(BCRYPT_COST_12_PATTERN);
        assertThat(encoded).startsWith("$2a$12$");
    }
}
