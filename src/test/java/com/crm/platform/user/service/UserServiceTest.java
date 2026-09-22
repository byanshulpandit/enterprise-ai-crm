package com.crm.platform.user.service;

import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("1. findById success")
    void testFindByIdSuccess() {
        User user = new User("admin1", "admin1@example.com", "hash", RoleEnum.ROLE_ADMIN);
        user.setId(100L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));

        User result = userService.findById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUsername()).isEqualTo("admin1");
    }

    @Test
    @DisplayName("2. findById missing throws ResourceNotFoundException")
    void testFindByIdMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        assertThatThrownBy(() -> userService.findById(null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("3. findByUsername success")
    void testFindByUsernameSuccess() {
        User user = new User("marketer_user", "m@example.com", "hash", RoleEnum.ROLE_MARKETER);
        when(userRepository.findByUsername("marketer_user")).thenReturn(Optional.of(user));

        User result = userService.findByUsername("marketer_user");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("marketer_user");
        assertThat(result.getRole()).isEqualTo(RoleEnum.ROLE_MARKETER);

        assertThatThrownBy(() -> userService.findByUsername("absent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("4. findByEmail success")
    void testFindByEmailSuccess() {
        User user = new User("user_email", "user@example.com", "hash", RoleEnum.ROLE_MARKETER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        User result = userService.findByEmail("user@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("user@example.com");

        assertThatThrownBy(() -> userService.findByEmail("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("5. findByUsernameOrEmail success")
    void testFindByUsernameOrEmailSuccess() {
        User userByUsername = new User("found_user", "found@example.com", "hash", RoleEnum.ROLE_ADMIN);
        when(userRepository.findByUsernameOrEmail(eq("found_user"), eq("found_user"))).thenReturn(Optional.of(userByUsername));

        User result1 = userService.findByUsernameOrEmail("found_user");
        assertThat(result1.getUsername()).isEqualTo("found_user");

        when(userRepository.findByUsernameOrEmail(eq("unknown"), eq("unknown"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findByUsernameOrEmail("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("6. countUsers returns total user count")
    void testCountUsers() {
        when(userRepository.count()).thenReturn(5L);

        long count = userService.countUsers();

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("7. countAdmins returns admin count")
    void testCountAdmins() {
        when(userRepository.countByRole(RoleEnum.ROLE_ADMIN)).thenReturn(2L);

        long admins = userService.countAdmins();

        assertThat(admins).isEqualTo(2L);
    }

    @Test
    @DisplayName("8. validateActiveUser succeeds for active user")
    void testValidateActiveUser() {
        User activeUser = new User("active_user", "active@example.com", "hash", RoleEnum.ROLE_MARKETER, true);

        assertThatCode(() -> userService.validateActiveUser(activeUser))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("9. validateActiveUser rejects inactive user with InvalidRequestException")
    void testValidateActiveUserRejectsInactive() {
        User inactiveUser = new User("inactive_user", "inactive@example.com", "hash", RoleEnum.ROLE_MARKETER, false);

        assertThatThrownBy(() -> userService.validateActiveUser(inactiveUser))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("inactive");

        assertThatThrownBy(() -> userService.validateActiveUser(null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
