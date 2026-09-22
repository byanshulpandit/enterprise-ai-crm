package com.crm.platform.security;

import com.crm.platform.common.exception.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PasswordValidatorTest {

    @Test
    @DisplayName("1. Null password is rejected")
    void testNullPasswordRejected() {
        assertThatThrownBy(() -> PasswordValidator.validate(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("between 8 and 72 characters");
    }

    @Test
    @DisplayName("2. 7 characters password is rejected (< 8 chars)")
    void testSevenCharsRejected() {
        assertThatThrownBy(() -> PasswordValidator.validate("1234567"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("between 8 and 72 characters");
    }

    @Test
    @DisplayName("3. 8 characters password is accepted (minimum boundary)")
    void testEightCharsAccepted() {
        assertThatCode(() -> PasswordValidator.validate("12345678"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("4. Standard valid password is accepted")
    void testValidPasswordAccepted() {
        assertThatCode(() -> PasswordValidator.validate("SuperSecretPassword123!"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("5. Exactly 72 ASCII characters (72 bytes) is accepted (maximum boundary)")
    void testExactly72AsciiBytesAccepted() {
        String pass72 = "A".repeat(72);
        assertThat(pass72.length()).isEqualTo(72);
        assertThat(pass72.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);

        assertThatCode(() -> PasswordValidator.validate(pass72))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("6. 73 ASCII characters (73 bytes) is rejected (> 72 characters)")
    void test73AsciiCharsRejected() {
        String pass73 = "A".repeat(73);
        assertThat(pass73.length()).isEqualTo(73);

        assertThatThrownBy(() -> PasswordValidator.validate(pass73))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("between 8 and 72 characters");
    }

    @Test
    @DisplayName("7. Multibyte UTF-8 password: 24 3-byte characters = 24 chars and exactly 72 bytes -> accepted")
    void testMultibyteUtf8_Exactly72BytesAccepted() {
        // '€' is 3 UTF-8 bytes: 24 * 3 = 72 bytes, 24 characters (between 8 and 72)
        String pass72Bytes = "€".repeat(24);
        assertThat(pass72Bytes.length()).isEqualTo(24);
        assertThat(pass72Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);

        assertThatCode(() -> PasswordValidator.validate(pass72Bytes))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("8. Multibyte UTF-8 password: 25 3-byte characters = 25 chars and 75 bytes -> rejected (> 72 UTF-8 bytes)")
    void testMultibyteUtf8_Exceeding72BytesRejected() {
        // '€' is 3 UTF-8 bytes: 25 * 3 = 75 bytes, 25 characters (within 8..72 char range, but exceeds 72 bytes)
        String pass75Bytes = "€".repeat(25);
        assertThat(pass75Bytes.length()).isEqualTo(25);
        assertThat(pass75Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(75);

        assertThatThrownBy(() -> PasswordValidator.validate(pass75Bytes))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not exceed 72 UTF-8 bytes");
    }

    @Test
    @DisplayName("9. Multibyte UTF-8 password: 71 ASCII chars + 1 2-byte character = 72 chars and 73 bytes -> rejected")
    void test72Chars73BytesRejected() {
        // 'ñ' is 2 UTF-8 bytes. 71 ASCII chars + 'ñ' = 72 characters, but 73 UTF-8 bytes
        String pass73Bytes = "a".repeat(71) + "ñ";
        assertThat(pass73Bytes.length()).isEqualTo(72);
        assertThat(pass73Bytes.getBytes(StandardCharsets.UTF_8).length).isEqualTo(73);

        assertThatThrownBy(() -> PasswordValidator.validate(pass73Bytes))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not exceed 72 UTF-8 bytes");
    }
}
