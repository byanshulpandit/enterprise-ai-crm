package com.crm.platform.frontend;

import com.crm.platform.frontend.security.UserSession;
import com.crm.platform.user.entity.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Frontend UserSession Unit Tests")
class UserSessionTest {

    private UserSession userSession;

    @BeforeEach
    void setUp() {
        userSession = new UserSession();
    }

    @Test
    @DisplayName("Should be unauthenticated by default")
    void shouldBeUnauthenticatedByDefault() {
        assertFalse(userSession.isLoggedIn());
        assertNull(userSession.getToken());
        assertNull(userSession.getUsername());
        assertNull(userSession.getRole());
        assertFalse(userSession.isAdmin());
        assertFalse(userSession.isMarketer());
    }

    @Test
    @DisplayName("Should record authenticated state and detect ADMIN role")
    void shouldAuthenticateAdmin() {
        userSession = new UserSession(1L, "admin_user", "admin@domain.com", RoleEnum.ROLE_ADMIN, "jwt-token-123");

        assertTrue(userSession.isLoggedIn());
        assertEquals("jwt-token-123", userSession.getToken());
        assertEquals("admin_user", userSession.getUsername());
        assertEquals("admin@domain.com", userSession.getEmail());
        assertEquals(RoleEnum.ROLE_ADMIN, userSession.getRole());
        assertTrue(userSession.isAdmin());
        assertFalse(userSession.isMarketer());
        assertTrue(userSession.hasRole(RoleEnum.ROLE_ADMIN));
        assertTrue(userSession.hasRole(RoleEnum.ROLE_MARKETER)); // Admin inherits marketer rights
    }

    @Test
    @DisplayName("Should record authenticated state and detect MARKETER role")
    void shouldAuthenticateMarketer() {
        userSession = new UserSession(2L, "marketing_lead", "lead@domain.com", RoleEnum.ROLE_MARKETER, "jwt-token-456");

        assertTrue(userSession.isLoggedIn());
        assertEquals("marketing_lead", userSession.getUsername());
        assertEquals(RoleEnum.ROLE_MARKETER, userSession.getRole());
        assertFalse(userSession.isAdmin());
        assertTrue(userSession.isMarketer());
        assertFalse(userSession.hasRole(RoleEnum.ROLE_ADMIN));
        assertTrue(userSession.hasRole(RoleEnum.ROLE_MARKETER));
    }

    @Test
    @DisplayName("Should clear session on logout")
    void shouldClearSessionOnLogout() {
        userSession = new UserSession(1L, "admin_user", "admin@domain.com", RoleEnum.ROLE_ADMIN, "jwt-token-123");
        assertTrue(userSession.isLoggedIn());

        userSession.clear();

        assertFalse(userSession.isLoggedIn());
        assertNull(userSession.getToken());
        assertNull(userSession.getUsername());
        assertNull(userSession.getEmail());
        assertNull(userSession.getRole());
    }
}
