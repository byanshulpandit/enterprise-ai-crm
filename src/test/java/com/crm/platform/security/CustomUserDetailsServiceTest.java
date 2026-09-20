package com.crm.platform.security;

import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("1. Load user by canonical username")
    void testLoadByUsername() {
        User user = new User("admin_user", "admin@example.com", "$2a$12$hashedPassword", RoleEnum.ROLE_ADMIN);
        when(userRepository.findByUsernameOrEmail(eq("admin_user"), eq("admin_user"))).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin_user");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin_user");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("2. Load user by email identifier")
    void testLoadByEmail() {
        User user = new User("marketer_user", "marketer@example.com", "$2a$12$hashedPassword", RoleEnum.ROLE_MARKETER);
        when(userRepository.findByUsernameOrEmail(eq("marketer@example.com"), eq("marketer@example.com"))).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("marketer@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("marketer_user");
    }

    @Test
    @DisplayName("3. Username in UserDetails is always the canonical User.username even when looked up by email")
    void testUsernameBecomesCanonicalUserDetailsUsername() {
        User user = new User("canonical_username", "lookup_email@example.com", "$2a$12$hash", RoleEnum.ROLE_ADMIN);
        when(userRepository.findByUsernameOrEmail(eq("lookup_email@example.com"), eq("lookup_email@example.com")))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("lookup_email@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("canonical_username");
        assertThat(userDetails.getUsername()).isNotEqualTo("lookup_email@example.com");
    }

    @Test
    @DisplayName("4. Password hash is passed through unchanged")
    void testPasswordHashPassedThroughUnchanged() {
        String exactHash = "$2a$12$ExactDbPasswordHashStoredInDatabase";
        User user = new User("user1", "user1@example.com", exactHash, RoleEnum.ROLE_ADMIN);
        when(userRepository.findByUsernameOrEmail(eq("user1"), eq("user1"))).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("user1");

        assertThat(userDetails.getPassword()).isEqualTo(exactHash);
    }

    @Test
    @DisplayName("5. ROLE_ADMIN authority is mapped correctly")
    void testRoleAdminAuthorityIsCorrect() {
        User user = new User("admin1", "admin1@example.com", "$2a$12$hash", RoleEnum.ROLE_ADMIN);
        when(userRepository.findByUsernameOrEmail(eq("admin1"), eq("admin1"))).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin1");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("6. ROLE_MARKETER authority is mapped correctly")
    void testRoleMarketerAuthorityIsCorrect() {
        User user = new User("marketer1", "marketer1@example.com", "$2a$12$hash", RoleEnum.ROLE_MARKETER);
        when(userRepository.findByUsernameOrEmail(eq("marketer1"), eq("marketer1"))).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("marketer1");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MARKETER");
    }

    @Test
    @DisplayName("7. Missing or blank identifier throws UsernameNotFoundException")
    void testMissingIdentifierThrowsUsernameNotFoundException() {
        when(userRepository.findByUsernameOrEmail(eq("nonexistent"), eq("nonexistent"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nonexistent");

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("8. Inactive user maps to UserDetails.enabled == false")
    void testInactiveUserReturnsDisabledUserDetails() {
        User user = new User("inactive_user", "inactive@example.com", "$2a$12$hash", RoleEnum.ROLE_MARKETER, false);
        when(userRepository.findByUsernameOrEmail(eq("inactive_user"), eq("inactive_user"))).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactive_user");

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }
}
