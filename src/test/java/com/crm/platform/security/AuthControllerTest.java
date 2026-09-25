package com.crm.platform.security;

import com.crm.platform.security.dto.LoginRequest;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

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

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        if (jdbcTemplate != null) {
            jdbcTemplate.update("DELETE FROM campaigns");
            jdbcTemplate.update("DELETE FROM segments");
        }
        userRepository.deleteAll();

        // Active admin
        User admin = new User("admin_user", "admin@crm.internal", passwordEncoder.encode("AdminPass123!"), RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        userRepository.save(admin);

        // Active marketer
        User marketer = new User("marketer_sara", "sara@crm.internal", passwordEncoder.encode("MarketerPass123!"), RoleEnum.ROLE_MARKETER, Boolean.TRUE);
        userRepository.save(marketer);

        // Inactive user
        User inactive = new User("inactive_user", "inactive@crm.internal", passwordEncoder.encode("InactivePass123!"), RoleEnum.ROLE_MARKETER, Boolean.FALSE);
        userRepository.save(inactive);
    }

    @Test
    @DisplayName("1. Successful login with canonical username")
    void testSuccessfulLoginWithUsername() throws Exception {
        LoginRequest request = new LoginRequest("admin_user", "AdminPass123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.username", is("admin_user")))
                .andExpect(jsonPath("$.data.user.role", is("ROLE_ADMIN")))
                .andReturn();

        // Verify sensitive information not returned
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("AdminPass123!");
    }

    @Test
    @DisplayName("2. Successful login with email")
    void testSuccessfulLoginWithEmail() throws Exception {
        LoginRequest request = new LoginRequest("sara@crm.internal", "MarketerPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.username", is("marketer_sara")))
                .andExpect(jsonPath("$.data.user.role", is("ROLE_MARKETER")));
    }

    @Test
    @DisplayName("3. JWT sub always contains canonical username even when login used email")
    void testJwtSubAlwaysCanonicalUsername() throws Exception {
        LoginRequest request = new LoginRequest("admin@crm.internal", "AdminPass123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseContent).path("data").path("token").asText();

        assertThat(token).isNotEmpty();
        String subject = jwtTokenProvider.extractUsername(token);
        assertThat(subject).isEqualTo("admin_user"); // Canonical username, NOT the email!
    }

    @Test
    @DisplayName("4. Wrong password returns 401 Unauthorized with standard error envelope")
    void testWrongPasswordReturns401() throws Exception {
        LoginRequest request = new LoginRequest("admin_user", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Invalid username or password")));
    }

    @Test
    @DisplayName("5. Unknown user returns 401 Unauthorized without leaking existence")
    void testUnknownUserReturns401() throws Exception {
        LoginRequest request = new LoginRequest("non_existent_user", "SomePassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Invalid username or password")));
    }

    @Test
    @DisplayName("6. Inactive user returns 401 Unauthorized")
    void testInactiveUserReturns401() throws Exception {
        LoginRequest request = new LoginRequest("inactive_user", "InactivePass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Invalid username or password")));
    }

    @Test
    @DisplayName("7. Invalid request body returns 400 Bad Request with VALIDATION_FAILED")
    void testInvalidRequestReturns400() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("8. Stateless: login does not create an HTTP session")
    void testNoHttpSessionCreated() throws Exception {
        LoginRequest request = new LoginRequest("admin_user", "AdminPass123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getCookie("JSESSIONID")).isNull();
    }
}
