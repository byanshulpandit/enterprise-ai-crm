package com.crm.platform.frontend;

import com.crm.platform.frontend.security.RouteAccessChecker;
import com.crm.platform.frontend.views.admin.UserManagementView;
import com.crm.platform.frontend.views.customers.CustomerListView;
import com.crm.platform.frontend.views.login.LoginView;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RouteAccessChecker Reflection & Rule Tests")
class RouteAccessCheckerTest {

    @RolesAllowed("ROLE_ADMIN")
    static class AdminAnnotatedView {}

    @RolesAllowed({"MARKETER", "ROLE_ADMIN"})
    static class MultiRoleView {}

    @RolesAllowed("MARKETER")
    static class MarketerOnlyView {}

    static class PublicView {}

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
        assertFalse(CustomerListView.class.getName().contains(".views.admin."));
        assertFalse(LoginView.class.getName().contains(".views.admin."));
    }
}
