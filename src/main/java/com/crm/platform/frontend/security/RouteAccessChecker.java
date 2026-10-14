package com.crm.platform.frontend.security;

import com.crm.platform.frontend.views.errors.AccessDeniedView;
import com.crm.platform.frontend.views.login.LoginView;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Component;

/**
 * Route access checker enforcing authentication and role-based access control (RBAC) across all views.
 */
@Component
public class RouteAccessChecker implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            uiEvent.getUI().addBeforeEnterListener(enterEvent -> {
                Class<?> target = enterEvent.getNavigationTarget();

                boolean isLoginView = LoginView.class.equals(target);
                boolean isAuthenticated = FrontendSecurityContext.isAuthenticated();

                if (isLoginView) {
                    if (isAuthenticated) {
                        enterEvent.forwardTo("");
                    }
                    return;
                }

                if (!isAuthenticated) {
                    enterEvent.forwardTo(LoginView.class);
                    return;
                }

                // Check admin access restrictions
                boolean requiresAdmin = target.getName().contains(".views.admin.") ||
                        isAnnotatedWithAdmin(target);

                if (requiresAdmin && !FrontendSecurityContext.isAdmin()) {
                    enterEvent.forwardTo(AccessDeniedView.class);
                }
            });
        });
    }

    private boolean isAnnotatedWithAdmin(Class<?> target) {
        if (target.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed annotation = target.getAnnotation(RolesAllowed.class);
            for (String role : annotation.value()) {
                if ("ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role)) {
                    return true;
                }
            }
        }
        return false;
    }
}
