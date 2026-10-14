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
        return threadSession != null ? threadSession : new UserSession();
    }

    public static void setCurrentSession(UserSession userSession) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(UserSession.class, userSession);
            // Ensure thread-local fallback is cleaned up to prevent worker thread contamination
            fallbackSession.remove();
        } else {
            if (userSession != null) {
                fallbackSession.set(userSession);
            } else {
                fallbackSession.remove();
            }
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

    public static void clearThreadSession() {
        UserSession threadSession = fallbackSession.get();
        if (threadSession != null) {
            threadSession.clear();
        }
        fallbackSession.remove();
    }

    /**
     * Safely executes an action with the specified UserSession bound to the current thread,
     * restoring or removing the thread-local state in a guaranteed finally block.
     */
    public static void runWithSession(UserSession session, Runnable action) {
        UserSession previous = fallbackSession.get();
        try {
            if (session != null) {
                fallbackSession.set(session);
            } else {
                fallbackSession.remove();
            }
            action.run();
        } finally {
            if (previous != null) {
                fallbackSession.set(previous);
            } else {
                fallbackSession.remove();
            }
        }
    }

    /**
     * Safely executes a supplier with the specified UserSession bound to the current thread,
     * restoring or removing the thread-local state in a guaranteed finally block.
     */
    public static <T> T supplyWithSession(UserSession session, java.util.function.Supplier<T> supplier) {
        UserSession previous = fallbackSession.get();
        try {
            if (session != null) {
                fallbackSession.set(session);
            } else {
                fallbackSession.remove();
            }
            return supplier.get();
        } finally {
            if (previous != null) {
                fallbackSession.set(previous);
            } else {
                fallbackSession.remove();
            }
        }
    }

    public static void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            UserSession userSession = session.getAttribute(UserSession.class);
            if (userSession != null) {
                userSession.clear();
            }
            session.setAttribute(UserSession.class, null);
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
        clearThreadSession();
    }
}
