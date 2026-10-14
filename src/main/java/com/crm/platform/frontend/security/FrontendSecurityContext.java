package com.crm.platform.frontend.security;

import com.crm.platform.user.entity.RoleEnum;
import com.vaadin.flow.server.VaadinSession;

import java.util.Optional;

/**
 * Accessor for the active UserSession stored within the thread-local VaadinSession.
 */
public final class FrontendSecurityContext {

    private static final ThreadLocal<UserSession> fallbackSession = new ThreadLocal<>();

    private FrontendSecurityContext() {
    }

    public static UserSession getCurrentSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            UserSession userSession = session.getAttribute(UserSession.class);
            if (userSession == null) {
                userSession = new UserSession();
                session.setAttribute(UserSession.class, userSession);
            }
            return userSession;
        }
        UserSession threadSession = fallbackSession.get();
        if (threadSession == null) {
            threadSession = new UserSession();
            fallbackSession.set(threadSession);
        }
        return threadSession;
    }

    public static void setCurrentSession(UserSession userSession) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(UserSession.class, userSession);
        }
        if (userSession != null) {
            fallbackSession.set(userSession);
        } else {
            fallbackSession.remove();
        }
    }

    public static Optional<String> getCurrentToken() {
        UserSession session = getCurrentSession();
        if (session != null && session.isLoggedIn()) {
            return Optional.ofNullable(session.getToken());
        }
        return Optional.empty();
    }

    public static Optional<String> getCurrentUsername() {
        UserSession session = getCurrentSession();
        if (session != null && session.isLoggedIn()) {
            return Optional.ofNullable(session.getUsername());
        }
        return Optional.empty();
    }

    public static boolean isAuthenticated() {
        return getCurrentSession().isLoggedIn();
    }

    public static boolean hasRole(RoleEnum role) {
        return getCurrentSession().hasRole(role);
    }

    public static boolean isAdmin() {
        return getCurrentSession().isAdmin();
    }

    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            UserSession userSession = session.getAttribute(UserSession.class);
            if (userSession != null) {
                userSession.clear();
            }
            session.setAttribute(UserSession.class, null);
            session.close();
        }
        UserSession threadSession = fallbackSession.get();
        if (threadSession != null) {
            threadSession.clear();
        }
        fallbackSession.remove();
    }
}
