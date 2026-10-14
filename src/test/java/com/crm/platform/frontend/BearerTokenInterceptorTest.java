package com.crm.platform.frontend;

import com.crm.platform.frontend.security.BearerTokenInterceptor;
import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.security.UserSession;
import com.crm.platform.user.entity.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BearerTokenInterceptor Unit & Security Tests")
class BearerTokenInterceptorTest {

    private BearerTokenInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new BearerTokenInterceptor();
    }

    @Test
    @DisplayName("Should inject Bearer token into request headers when user is logged in")
    void shouldInjectBearerTokenWhenLoggedIn() throws IOException {
        UserSession session = new UserSession(1L, "admin", "admin@crm.internal", RoleEnum.ROLE_ADMIN, "test-jwt-token-12345");
        FrontendSecurityContext.setCurrentSession(session);

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/v1/customers"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(execution.execute(any(), any())).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertEquals("Bearer test-jwt-token-12345", headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("Should not inject Authorization header when user is unauthenticated")
    void shouldNotInjectAuthorizationWhenUnauthenticated() throws IOException {
        FrontendSecurityContext.setCurrentSession(new UserSession());

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/v1/customers"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(execution.execute(any(), any())).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("Should invalidate session on 401 response from non-login API endpoint")
    void shouldInvalidateSessionOn401NonLogin() throws IOException {
        UserSession session = new UserSession(1L, "marketer", "marketer@crm.internal", RoleEnum.ROLE_MARKETER, "expired-token");
        FrontendSecurityContext.setCurrentSession(session);
        assertTrue(session.isLoggedIn());

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/v1/customers"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
        when(execution.execute(any(), any())).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        // Session was logged out / cleared
        assertFalse(session.isLoggedIn());
        assertNull(session.getToken());
    }

    @Test
    @DisplayName("Should NOT invalidate session on 401 response from login endpoint")
    void shouldNotInvalidateSessionOn401LoginEndpoint() throws IOException {
        UserSession session = new UserSession(null, null, null, null, null);
        FrontendSecurityContext.setCurrentSession(session);

        HttpRequest request = mock(HttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/v1/auth/login"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
        when(execution.execute(any(), any())).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }
}
