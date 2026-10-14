package com.crm.platform.frontend;

import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.security.RouteAccessChecker;
import com.crm.platform.frontend.security.UserSession;
import com.crm.platform.frontend.views.admin.AiAuditsView;
import com.crm.platform.frontend.views.admin.UserManagementView;
import com.crm.platform.frontend.views.campaigns.CampaignListView;
import com.crm.platform.frontend.views.customers.CustomerListView;
import com.crm.platform.frontend.views.dashboard.DashboardView;
import com.crm.platform.frontend.views.login.LoginView;
import com.crm.platform.user.entity.RoleEnum;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RouteAccessChecker & RBAC Policy Tests")
class RouteAccessCheckerTest {

    @RolesAllowed("ROLE_ADMIN")
    static class AdminAnnotatedView {}

    @RolesAllowed({"MARKETER", "ROLE_ADMIN"})
    static class MultiRoleView {}

    @RolesAllowed("MARKETER")
    static class MarketerOnlyView {}

    static class PublicView {}

    @BeforeEach
    void setUp() {
        FrontendSecurityContext.logout();
    }

    @Test
    @DisplayName("Should detect admin package views and annotations")
    void shouldDetectAdminPackageAndAnnotation() throws Exception {
        RouteAccessChecker checker = new RouteAccessChecker();
        Method isAnnotated = RouteAccessChecker.class.getDeclaredMethod("isAnnotatedWithAdmin", Class.class);
        isAnnotated.setAccessible(true);

        assertTrue((Boolean) isAnnotated.invoke(checker, AdminAnnotatedView.class));
        assertTrue((Boolean) isAnnotated.invoke(checker, MultiRoleView.class));
        assertFalse((Boolean) isAnnotated.invoke(checker, MarketerOnlyView.class));
        assertFalse((Boolean) isAnnotated.invoke(checker, PublicView.class));

        // Package matching test
        assertTrue(UserManagementView.class.getName().contains(".views.admin."));
        assertTrue(AiAuditsView.class.getName().contains(".views.admin."));
        assertFalse(CustomerListView.class.getName().contains(".views.admin."));
        assertFalse(CampaignListView.class.getName().contains(".views.admin."));
        assertFalse(DashboardView.class.getName().contains(".views.admin."));
        assertFalse(LoginView.class.getName().contains(".views.admin."));
    }

    @Test
    @DisplayName("Should verify actual views have proper server-side @RolesAllowed annotations")
    void verifyActualViewAnnotations() {
        assertTrue(UserManagementView.class.isAnnotationPresent(RolesAllowed.class));
        RolesAllowed userMgmtRoles = UserManagementView.class.getAnnotation(RolesAllowed.class);
        boolean hasAdmin = false;
        for (String r : userMgmtRoles.value()) {
            if ("ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r)) {
                hasAdmin = true;
            }
        }
        assertTrue(hasAdmin, "UserManagementView must require ADMIN role");

        assertTrue(AiAuditsView.class.isAnnotationPresent(RolesAllowed.class));
        RolesAllowed aiAuditsRoles = AiAuditsView.class.getAnnotation(RolesAllowed.class);
        boolean aiHasAdmin = false;
        for (String r : aiAuditsRoles.value()) {
            if ("ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r)) {
                aiHasAdmin = true;
            }
        }
        assertTrue(aiHasAdmin, "AiAuditsView must require ADMIN role");
    }

    @Test
    @DisplayName("Should enforce role hierarchy where ADMIN inherits MARKETER permissions")
    void testAdminRoleHierarchy() {
        UserSession adminSession = new UserSession(1L, "admin", "admin@crm.internal", RoleEnum.ROLE_ADMIN, "valid-token");
        FrontendSecurityContext.setCurrentSession(adminSession);

        assertTrue(FrontendSecurityContext.isAuthenticated());
        assertTrue(FrontendSecurityContext.isAdmin());
        assertTrue(adminSession.hasRole(RoleEnum.ROLE_ADMIN));
        assertTrue(adminSession.hasRole(RoleEnum.ROLE_MARKETER), "Admin must inherit Marketer permissions");
    }

    @Test
    @DisplayName("Should enforce non-admin MARKETER cannot access ADMIN actions")
    void testMarketerCannotAccessAdmin() {
        UserSession marketerSession = new UserSession(2L, "marketer", "marketer@crm.internal", RoleEnum.ROLE_MARKETER, "valid-token");
        FrontendSecurityContext.setCurrentSession(marketerSession);

        assertTrue(FrontendSecurityContext.isAuthenticated());
        assertFalse(FrontendSecurityContext.isAdmin());
        assertFalse(marketerSession.isAdmin());
        assertTrue(marketerSession.hasRole(RoleEnum.ROLE_MARKETER));
        assertFalse(marketerSession.hasRole(RoleEnum.ROLE_ADMIN), "Marketer must not possess Admin authority");
    }

    @Test
    @DisplayName("Should properly report unauthenticated state after logout")
    void testLogoutClearsSession() {
        UserSession session = new UserSession(1L, "admin", "admin@crm.internal", RoleEnum.ROLE_ADMIN, "valid-token");
        FrontendSecurityContext.setCurrentSession(session);
        assertTrue(FrontendSecurityContext.isAuthenticated());

        FrontendSecurityContext.logout();
        assertFalse(FrontendSecurityContext.isAuthenticated());
        assertFalse(FrontendSecurityContext.isAdmin());
    }
}
