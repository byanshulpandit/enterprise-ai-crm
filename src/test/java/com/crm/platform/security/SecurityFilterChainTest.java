package com.crm.platform.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private SecurityAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private SecurityAccessDeniedHandler accessDeniedHandler;

    @Test
    @DisplayName("1. Security configuration and filter chain beans load successfully")
    void testSecurityConfigurationLoadsSuccessfully() {
        assertThat(securityConfig).isNotNull();
        assertThat(securityFilterChain).isNotNull();
        assertThat(authenticationEntryPoint).isNotNull();
        assertThat(accessDeniedHandler).isNotNull();
        assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
    }

    @Test
    @DisplayName("2. AuthenticationEntryPoint is wired and returns 401 on unauthenticated access to protected endpoint")
    void testAuthenticationEntryPointWiredOnUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.success", is(false)))

                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Authentication required. A valid Bearer token must be provided.")))
                .andExpect(jsonPath("$.error.timestamp").isString())
                .andExpect(jsonPath("$.error.requestId").isString());
    }

    @Test
    @DisplayName("3. Public auth endpoint (/api/v1/auth/login) is permitted without authentication in the filter chain")
    void testPublicLoginEndpointPermittedWithoutAuthentication() throws Exception {
        // Public endpoint passes security filter chain without Authorization header and reaches AuthController
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("4. CSRF is disabled: allows protected state-changing POST (/api/v1/customers) without CSRF token")
    @WithMockUser(roles = "MARKETER")
    void testCsrfDisabled_AllowsProtectedPostWithoutCsrfToken_NotRejectedWithForbidden() throws Exception {
        // Protected state-changing endpoint POST /api/v1/customers with @WithMockUser and NO CSRF token.
        // If CSRF were active, Spring Security would reject this request with 403 Forbidden before reaching controller.
        // With CSRF disabled, the request is not rejected with 403; it passes security and reaches the controller layer.
        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("5. HTTP Basic is disabled: does not return WWW-Authenticate Basic header on unauthenticated request")
    void testHttpBasicDisabledDoesNotSendBasicChallenge() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA=="))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("6. Form login is disabled: does not redirect to login page")
    void testFormLoginDisabledDoesNotRedirect() throws Exception {
        MvcResult result = mockMvc.perform(get("/login"))
                .andReturn();

        // Form login would return 200 HTML or 302 redirect. Disabled form login means it's treated as protected endpoint -> 401 JSON
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("7. Session creation policy is STATELESS: does not create HTTP sessions for authentication")
    @WithMockUser(roles = "MARKETER")
    void testSessionCreationPolicyIsStateless() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("8. AccessDeniedHandler and AuthenticationEntryPoint are wired into SecurityConfig")
    void testHandlersAreWiredIntoSecurityConfig() {
        assertThat(securityConfig).hasFieldOrPropertyWithValue("authenticationEntryPoint", authenticationEntryPoint);
        assertThat(securityConfig).hasFieldOrPropertyWithValue("accessDeniedHandler", accessDeniedHandler);
    }
}
