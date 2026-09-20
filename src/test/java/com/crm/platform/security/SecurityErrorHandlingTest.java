package com.crm.platform.security;

import com.crm.platform.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class SecurityErrorHandlingTest {

    private ObjectMapper objectMapper;
    private SecurityAuthenticationEntryPoint authenticationEntryPoint;
    private SecurityAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authenticationEntryPoint = new SecurityAuthenticationEntryPoint(objectMapper);
        accessDeniedHandler = new SecurityAccessDeniedHandler(objectMapper);
    }

    // ==========================================
    // SecurityAuthenticationEntryPoint Tests (401)
    // ==========================================

    @Test
    @DisplayName("AuthenticationEntryPoint returns HTTP 401 Unauthorized")
    void authenticationEntryPointReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new InsufficientAuthenticationException("Full authentication is required");

        authenticationEntryPoint.commence(request, response, authException);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("AuthenticationEntryPoint sets Content-Type to application/json and encoding to UTF-8")
    void authenticationEntryPointSetsJsonContentType() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response, new InsufficientAuthenticationException("No token"));

        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    }

    @Test
    @DisplayName("AuthenticationEntryPoint produces standard ErrorResponse structure")
    void authenticationEntryPointProducesStandardErrorResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response, new InsufficientAuthenticationException("Full authentication is required"));

        String responseBody = response.getContentAsString();
        assertThat(responseBody).isNotBlank();

        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getError()).isNotNull();
        assertThat(errorResponse.getError().getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(errorResponse.getError().getMessage()).isEqualTo("Authentication required. A valid Bearer token must be provided.");
        assertThat(errorResponse.getError().getTimestamp()).isNotBlank();
        assertThat(errorResponse.getError().getRequestId()).isNotBlank();
        assertThatCode(() -> UUID.fromString(errorResponse.getError().getRequestId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("AuthenticationEntryPoint does not leak internal exception details or stack traces")
    void authenticationEntryPointDoesNotLeakSensitiveDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String sensitiveInternalMessage = "JWT expired at 2026-09-20T22:00:00Z with key ID hs256-internal-key-999";
        AuthenticationException authException = new BadCredentialsException(sensitiveInternalMessage);

        authenticationEntryPoint.commence(request, response, authException);

        String responseBody = response.getContentAsString();
        assertThat(responseBody).doesNotContain(sensitiveInternalMessage);
        assertThat(responseBody).doesNotContain("BadCredentialsException");
        assertThat(responseBody).doesNotContain("at com.crm.platform");
        assertThat(responseBody).doesNotContain("stackTrace");
    }

    @Test
    @DisplayName("AuthenticationEntryPoint handles null request and null exception gracefully")
    void authenticationEntryPointHandlesNullArgumentsGracefully() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatCode(() -> authenticationEntryPoint.commence(null, response, null))
                .doesNotThrowAnyException();

        assertThat(response.getStatus()).isEqualTo(401);
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(errorResponse.getError().getCode()).isEqualTo("UNAUTHORIZED");
    }

    // ==========================================
    // SecurityAccessDeniedHandler Tests (403)
    // ==========================================

    @Test
    @DisplayName("AccessDeniedHandler returns HTTP 403 Forbidden")
    void accessDeniedHandlerReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/customers/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access is denied");

        accessDeniedHandler.handle(request, response, accessDeniedException);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("AccessDeniedHandler sets Content-Type to application/json and encoding to UTF-8")
    void accessDeniedHandlerSetsJsonContentType() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/customers/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Denied"));

        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    }

    @Test
    @DisplayName("AccessDeniedHandler produces standard ErrorResponse structure")
    void accessDeniedHandlerProducesStandardErrorResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/customers/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access is denied"));

        String responseBody = response.getContentAsString();
        assertThat(responseBody).isNotBlank();

        ErrorResponse errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.isSuccess()).isFalse();
        assertThat(errorResponse.getError()).isNotNull();
        assertThat(errorResponse.getError().getCode()).isEqualTo("FORBIDDEN");
        assertThat(errorResponse.getError().getMessage()).isEqualTo("Access denied. You do not have permission to access this resource.");
        assertThat(errorResponse.getError().getTimestamp()).isNotBlank();
        assertThat(errorResponse.getError().getRequestId()).isNotBlank();
        assertThatCode(() -> UUID.fromString(errorResponse.getError().getRequestId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("AccessDeniedHandler does not leak security expressions, role requirements, or stack traces")
    void accessDeniedHandlerDoesNotLeakSensitiveDetails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/customers/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String sensitiveInternalMessage = "EL1008E: Property or field 'ROLE_ADMIN' not accessible on object of type 'UserPrincipal'";
        AccessDeniedException accessDeniedException = new AccessDeniedException(sensitiveInternalMessage);

        accessDeniedHandler.handle(request, response, accessDeniedException);

        String responseBody = response.getContentAsString();
        assertThat(responseBody).doesNotContain(sensitiveInternalMessage);
        assertThat(responseBody).doesNotContain("AccessDeniedException");
        assertThat(responseBody).doesNotContain("at com.crm.platform");
        assertThat(responseBody).doesNotContain("stackTrace");
    }

    @Test
    @DisplayName("AccessDeniedHandler handles null request and null exception gracefully")
    void accessDeniedHandlerHandlesNullArgumentsGracefully() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatCode(() -> accessDeniedHandler.handle(null, response, null))
                .doesNotThrowAnyException();

        assertThat(response.getStatus()).isEqualTo(403);
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(errorResponse.getError().getCode()).isEqualTo("FORBIDDEN");
    }
}
