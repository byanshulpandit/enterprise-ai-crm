package com.crm.platform.user.controller;

import com.crm.platform.security.JwtTokenProvider;
import com.crm.platform.user.dto.UserCreateRequest;
import com.crm.platform.user.dto.UserPasswordUpdateRequest;
import com.crm.platform.user.dto.UserRoleUpdateRequest;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUser = new User("admin_super", "admin@crm.internal", passwordEncoder.encode("AdminPass123!"), RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        adminUser = userRepository.save(adminUser);

        targetUser = new User("target_marketer", "target@crm.internal", passwordEncoder.encode("TargetPass123!"), RoleEnum.ROLE_MARKETER, Boolean.TRUE);
        targetUser = userRepository.save(targetUser);
    }

    @Test
    @DisplayName("1. ADMIN can create new user successfully with BCrypt hash and no exposed password")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminCanCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest("new_marketer", "new@crm.internal", "SecurePassword123!", RoleEnum.ROLE_MARKETER);

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.username", is("new_marketer")))
                .andExpect(jsonPath("$.data.email", is("new@crm.internal")))
                .andExpect(jsonPath("$.data.role", is("ROLE_MARKETER")))
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("passwordHash");
        assertThat(body).doesNotContain("SecurePassword123!");

        User saved = userRepository.findByUsername("new_marketer").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches("SecurePassword123!", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("2. MARKETER cannot create user: returns 403 Forbidden")
    @WithMockUser(username = "target_marketer", roles = "MARKETER")
    void testMarketerCannotCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest("another_user", "another@crm.internal", "SecurePassword123!", RoleEnum.ROLE_MARKETER);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("3. Unauthenticated cannot access user endpoints: returns 401 Unauthorized")
    void testUnauthenticatedCannotAccessUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("4. Duplicate username returns 409 Conflict")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testDuplicateUsernameConflict() throws Exception {
        UserCreateRequest request = new UserCreateRequest("admin_super", "unique@crm.internal", "SecurePassword123!", RoleEnum.ROLE_MARKETER);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")));
    }

    @Test
    @DisplayName("5. Duplicate email returns 409 Conflict")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testDuplicateEmailConflict() throws Exception {
        UserCreateRequest request = new UserCreateRequest("unique_user", "admin@crm.internal", "SecurePassword123!", RoleEnum.ROLE_MARKETER);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")));
    }

    @Test
    @DisplayName("6. Password policy violation (< 8 chars) returns 400 Bad Request")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testPasswordTooShort() throws Exception {
        UserCreateRequest request = new UserCreateRequest("short_pass", "short@crm.internal", "short", RoleEnum.ROLE_MARKETER);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("7. ADMIN can list users (paginated)")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.metadata.pagination.totalElements", is(2)));
    }

    @Test
    @DisplayName("8. ADMIN can get user by ID")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminCanGetUserById() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("target_marketer")));
    }

    @Test
    @DisplayName("9. ADMIN can update user role")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminCanUpdateUserRole() throws Exception {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(RoleEnum.ROLE_ADMIN);

        mockMvc.perform(patch("/api/v1/users/" + targetUser.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("ROLE_ADMIN")));

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
    }

    @Test
    @DisplayName("10. Admin self-demotion is rejected with 400 Bad Request")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminSelfDemotionRejected() throws Exception {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(RoleEnum.ROLE_MARKETER);

        mockMvc.perform(patch("/api/v1/users/" + adminUser.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.message", is("Administrators cannot demote their own account")));
    }

    @Test
    @DisplayName("11. Admin self-deactivation is rejected with 400 Bad Request")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminSelfDeactivationRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/users/" + adminUser.getId() + "/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.message", is("Administrators cannot deactivate their own account")));
    }

    @Test
    @DisplayName("12. ADMIN can deactivate another user")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminCanDeactivateOtherUser() throws Exception {
        mockMvc.perform(patch("/api/v1/users/" + targetUser.getId() + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isActive", is(false)));

        User inDb = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(inDb.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("13. Deactivated user is blocked on subsequent request even with previously valid JWT")
    void testDeactivatedUserBlockedOnNextRequest() throws Exception {
        // Generate valid JWT while user is active
        String jwtToken = jwtTokenProvider.generateToken(targetUser);

        // Deactivate user in DB
        targetUser.setIsActive(false);
        userRepository.save(targetUser);

        // Request protected endpoint using token
        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("14. Live DB role change takes effect immediately on next request with existing JWT")
    void testLiveDbRoleChangeTakesEffectOnNextRequest() throws Exception {
        // Initially targetUser is ROLE_MARKETER
        String jwtToken = jwtTokenProvider.generateToken(targetUser);

        // MARKETER cannot delete customers (403)
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isForbidden());

        // Elevate user in DB to ROLE_ADMIN
        targetUser.setRole(RoleEnum.ROLE_ADMIN);
        userRepository.save(targetUser);

        // Same JWT token now succeeds for ADMIN endpoint!
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("15. ADMIN can update user password with BCrypt hashing and validation")
    @WithMockUser(username = "admin_super", roles = "ADMIN")
    void testAdminUpdatePassword() throws Exception {
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("NewSuperPassword123!");

        mockMvc.perform(patch("/api/v1/users/" + targetUser.getId() + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.message", is("Password updated successfully")));

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSuperPassword123!", updated.getPasswordHash())).isTrue();
    }
}
