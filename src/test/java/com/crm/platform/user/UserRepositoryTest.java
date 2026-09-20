package com.crm.platform.user;

import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify User entity persists correctly with timestamps and default active status")
    void testPersistUser() {
        User user = new User("admin_test", "admin@example.com", "$2a$10$hashedpassword", RoleEnum.ROLE_ADMIN);
        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("admin_test");
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashedpassword");
        assertThat(saved.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Verify RoleEnum persists as VARCHAR string in MySQL")
    void testRoleEnumPersistenceAsString() {
        User user = new User("marketer_test", "marketer@example.com", "$2a$10$hashedpassword", RoleEnum.ROLE_MARKETER);
        User saved = userRepository.saveAndFlush(user);

        String roleInDb = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE id = ?",
                String.class,
                saved.getId()
        );
        assertThat(roleInDb).isEqualTo("ROLE_MARKETER");
    }

    @Test
    @DisplayName("Verify username uniqueness constraint in MySQL")
    void testUniqueUsername() {
        User u1 = new User("unique_user", "u1@example.com", "$2a$10$hash", RoleEnum.ROLE_MARKETER);
        userRepository.saveAndFlush(u1);

        User u2 = new User("unique_user", "u2@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN);
        assertThatThrownBy(() -> userRepository.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Verify email uniqueness constraint in MySQL")
    void testUniqueEmail() {
        User u1 = new User("user1", "same@example.com", "$2a$10$hash", RoleEnum.ROLE_MARKETER);
        userRepository.saveAndFlush(u1);

        User u2 = new User("user2", "same@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN);
        assertThatThrownBy(() -> userRepository.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Verify findByUsername query method")
    void testFindByUsername() {
        User user = new User("find_user", "find_user@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByUsername("find_user");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find_user@example.com");

        assertThat(userRepository.findByUsername("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("Verify findByEmail query method")
    void testFindByEmail() {
        User user = new User("find_email_user", "find_by_email@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmail("find_by_email@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("find_email_user");

        assertThat(userRepository.findByEmail("unknown@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Verify findByUsernameOrEmail query method")
    void testFindByUsernameOrEmail() {
        User user = new User("combo_user", "combo@example.com", "$2a$10$hash", RoleEnum.ROLE_MARKETER);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByUsernameOrEmail("combo_user", "other@example.com")).isPresent();
        assertThat(userRepository.findByUsernameOrEmail("other_user", "combo@example.com")).isPresent();
        assertThat(userRepository.findByUsernameOrEmail("other_user", "other@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Verify existsByUsername query method")
    void testExistsByUsername() {
        User user = new User("exist_user", "exist@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByUsername("exist_user")).isTrue();
        assertThat(userRepository.existsByUsername("absent_user")).isFalse();
    }

    @Test
    @DisplayName("Verify existsByEmail query method")
    void testExistsByEmail() {
        User user = new User("exist_email_user", "exist_email@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmail("exist_email@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("absent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Verify countByRole query method")
    void testCountByRole() {
        userRepository.saveAndFlush(new User("admin1", "a1@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN));
        userRepository.saveAndFlush(new User("admin2", "a2@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN));
        userRepository.saveAndFlush(new User("marketer1", "m1@example.com", "$2a$10$hash", RoleEnum.ROLE_MARKETER));

        assertThat(userRepository.countByRole(RoleEnum.ROLE_ADMIN)).isEqualTo(2);
        assertThat(userRepository.countByRole(RoleEnum.ROLE_MARKETER)).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify updatedAt updates on entity modification")
    void testUpdatedAtChangesOnUpdate() throws InterruptedException {
        User user = new User("update_user", "update@example.com", "$2a$10$hash", RoleEnum.ROLE_ADMIN);
        User saved = userRepository.saveAndFlush(user);

        Instant originalCreatedAt = saved.getCreatedAt();
        Instant originalUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(10);
        saved.setRole(RoleEnum.ROLE_MARKETER);
        User updated = userRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        assertThat(updated.getRole()).isEqualTo(RoleEnum.ROLE_MARKETER);
    }
}
