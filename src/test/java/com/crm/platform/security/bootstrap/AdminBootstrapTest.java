package com.crm.platform.security.bootstrap;

import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapProperties properties;
    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        properties = new AdminBootstrapProperties();
        properties.setUsername("bootstrap_admin");
        properties.setEmail("admin@crm.internal");
        properties.setPassword("BootstrapPass123!");

        runner = new AdminBootstrapRunner(userRepository, passwordEncoder, properties);
    }

    @Test
    @DisplayName("1. Empty DB (count == 0): creates exactly one configured ROLE_ADMIN")
    void testEmptyDbCreatesAdmin() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("BootstrapPass123!")).thenReturn("$2a$12$mockedHashedPassword");

        runner.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("bootstrap_admin");
        assertThat(saved.getEmail()).isEqualTo("admin@crm.internal");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$12$mockedHashedPassword");
        assertThat(saved.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("2. Non-empty DB (count > 0): does absolutely nothing")
    void testNonEmptyDbDoesNothing() {
        when(userRepository.count()).thenReturn(1L);

        runner.run(null);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("3. Subsequent startup when users exist (count == 5): does nothing")
    void testSubsequentStartupDoesNothing() {
        when(userRepository.count()).thenReturn(5L);

        runner.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. Missing required username when count == 0: fails safely with IllegalStateException")
    void testMissingUsernameFailsSafely() {
        when(userRepository.count()).thenReturn(0L);
        properties.setUsername(null);

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Missing required password when count == 0: fails safely with IllegalStateException")
    void testMissingPasswordFailsSafely() {
        when(userRepository.count()).thenReturn(0L);
        properties.setPassword("");

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Missing required email when count == 0: fails safely with IllegalStateException")
    void testMissingEmailFailsSafely() {
        when(userRepository.count()).thenReturn(0L);
        properties.setEmail("   ");

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Invalid password (< 8 chars) fails safely with InvalidRequestException")
    void testInvalidPasswordFailsSafely() {
        when(userRepository.count()).thenReturn(0L);
        properties.setPassword("short");

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("8 and 72");

        verify(userRepository, never()).save(any());
    }
}
