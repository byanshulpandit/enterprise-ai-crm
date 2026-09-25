package com.crm.platform.security;

import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. No Authorization header: continues chain without setting authentication")
    void testNoAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).isTokenValid(any());
    }

    @Test
    @DisplayName("2. Non-Bearer header: continues chain without setting authentication")
    void testNonBearerHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).isTokenValid(any());
    }

    @Test
    @DisplayName("3. Empty Bearer token: continues chain without setting authentication")
    void testEmptyBearerToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer    ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("4. Malformed / invalid token: continues chain without setting authentication")
    void testMalformedToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer malformed.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isTokenValid("malformed.token.here")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("5. Expired token: continues chain without setting authentication")
    void testExpiredToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isTokenValid("expired.jwt.token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("6. Invalid signature / wrong issuer: continues chain without setting authentication")
    void testInvalidSignatureOrWrongIssuer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer wrong.sig.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isTokenValid("wrong.sig.token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("7. Unknown DB user: continues chain without setting authentication")
    void testUnknownDbUser() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.isTokenValid("valid.token")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("valid.token")).thenReturn("ghost_user");
        when(userRepository.findByUsername("ghost_user")).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("8. Inactive DB user: continues chain without setting authentication")
    void testInactiveDbUser() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User inactiveUser = new User("inactive_user", "inactive@crm.internal", "hash", RoleEnum.ROLE_MARKETER, Boolean.FALSE);
        when(jwtTokenProvider.isTokenValid("valid.token")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("valid.token")).thenReturn("inactive_user");
        when(userRepository.findByUsername("inactive_user")).thenReturn(Optional.of(inactiveUser));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("9. Valid active user: populates SecurityContext with canonical username and DB role authority")
    void testValidActiveUserPopulatesSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User activeUser = new User("john_admin", "john@crm.internal", "hash", RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        when(jwtTokenProvider.isTokenValid("valid.token")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("valid.token")).thenReturn("john_admin");
        when(userRepository.findByUsername("john_admin")).thenReturn(Optional.of(activeUser));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo("john_admin");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("10. JWT role mismatch: DB role wins and is authoritative")
    void testJwtRoleMismatch_DbRoleWins() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.token.with.admin.claim");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // In DB, user was demoted or is ROLE_MARKETER
        User dbUser = new User("sara_marketer", "sara@crm.internal", "hash", RoleEnum.ROLE_MARKETER, Boolean.TRUE);
        when(jwtTokenProvider.isTokenValid("valid.token.with.admin.claim")).thenReturn(true);
        when(jwtTokenProvider.extractUsername("valid.token.with.admin.claim")).thenReturn("sara_marketer");
        when(userRepository.findByUsername("sara_marketer")).thenReturn(Optional.of(dbUser));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("sara_marketer");
        // Must have ROLE_MARKETER from DB, not any token claim
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_MARKETER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("11. Pre-existing authentication in SecurityContext: does not duplicate authentication")
    void testPreExistingAuthentication_DoesNotDuplicate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Authentication existing = new UsernamePasswordAuthenticationToken(
                "existing_user", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(existing);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(jwtTokenProvider, never()).isTokenValid(any());
        verify(filterChain).doFilter(request, response);
    }
}
