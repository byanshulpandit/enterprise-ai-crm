package com.crm.platform.frontend;

import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.security.UserSession;
import com.crm.platform.user.entity.RoleEnum;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FrontendSecurityContext Lifecycle & Thread Isolation Tests")
class FrontendSecurityContextTest {

    @BeforeEach
    void setUp() {
        VaadinSession.setCurrent(null);
        FrontendSecurityContext.clearThreadSession();
    }

    @AfterEach
    void tearDown() {
        VaadinSession.setCurrent(null);
        FrontendSecurityContext.clearThreadSession();
    }

    @Test
    @DisplayName("Single worker thread reuse: User A session never leaks into User B operation")
    void testSingleWorkerThreadReuseAcrossUsers() throws ExecutionException, InterruptedException {
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        try {
            UserSession userASession = new UserSession(1L, "user_a", "usera@crm.internal",
                    RoleEnum.ROLE_ADMIN, "jwt-token-user-a");

            // Task 1: Run as User A using runWithSession
            Future<String> task1 = singleThreadExecutor.submit(() ->
                    FrontendSecurityContext.supplyWithSession(userASession, () -> {
                        assertTrue(FrontendSecurityContext.isAuthenticated());
                        assertTrue(FrontendSecurityContext.isAdmin());
                        return FrontendSecurityContext.getCurrentToken().orElse(null);
                    })
            );
            assertEquals("jwt-token-user-a", task1.get());

            // Task 2: Run subsequent task on the EXACT SAME worker thread without any session
            Future<Optional<String>> task2 = singleThreadExecutor.submit(() -> {
                assertFalse(FrontendSecurityContext.isAuthenticated(), "Unauthenticated task must not see User A session");
                assertFalse(FrontendSecurityContext.isAdmin(), "Unauthenticated task must not possess Admin authority");
                return FrontendSecurityContext.getCurrentToken();
            });
            assertTrue(task2.get().isEmpty(), "User A's JWT token must never appear in subsequent task on reused thread");

            // Task 3: Run as User B on the same thread
            UserSession userBSession = new UserSession(2L, "user_b", "userb@crm.internal",
                    RoleEnum.ROLE_MARKETER, "jwt-token-user-b");

            Future<String> task3 = singleThreadExecutor.submit(() ->
                    FrontendSecurityContext.supplyWithSession(userBSession, () -> {
                        assertTrue(FrontendSecurityContext.isAuthenticated());
                        assertFalse(FrontendSecurityContext.isAdmin(), "User B is MARKETER, not ADMIN");
                        assertEquals("user_b", FrontendSecurityContext.getCurrentUsername().orElse(null));
                        return FrontendSecurityContext.getCurrentToken().orElse(null);
                    })
            );
            assertEquals("jwt-token-user-b", task3.get());

            // Task 4: Run after User B on same thread - must be unauthenticated
            Future<Boolean> task4 = singleThreadExecutor.submit(FrontendSecurityContext::isAuthenticated);
            assertFalse(task4.get(), "Thread must be completely clean after User B task completes");
        } finally {
            singleThreadExecutor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Worker thread reuse: Exception in User A operation reliably triggers cleanup")
    void testThreadReuseWhenUserAThrowsException() throws ExecutionException, InterruptedException {
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        try {
            UserSession userASession = new UserSession(1L, "user_a", "usera@crm.internal",
                    RoleEnum.ROLE_ADMIN, "jwt-token-user-a");

            // Task 1: User A throws an unhandled RuntimeException
            Future<?> task1 = singleThreadExecutor.submit(() ->
                    FrontendSecurityContext.runWithSession(userASession, () -> {
                        assertEquals("jwt-token-user-a", FrontendSecurityContext.getCurrentToken().orElse(null));
                        throw new IllegalStateException("Simulated crash in user A pipeline");
                    })
            );

            ExecutionException ex = assertThrows(ExecutionException.class, task1::get);
            assertTrue(ex.getCause() instanceof IllegalStateException);

            // Task 2: Run immediately on same worker thread, verify User A's session was cleaned up
            Future<Optional<String>> task2 = singleThreadExecutor.submit(FrontendSecurityContext::getCurrentToken);
            assertTrue(task2.get().isEmpty(), "Session must be reliably removed despite uncaught exception in User A task");

            Future<Boolean> task3 = singleThreadExecutor.submit(FrontendSecurityContext::isAuthenticated);
            assertFalse(task3.get(), "Must not be authenticated after exception cleanup");
        } finally {
            singleThreadExecutor.shutdownNow();
        }
    }

    @Test
    @DisplayName("No-VaadinSession behavior: Repeated reads do not allocate or leak ThreadLocal state")
    void testRepeatedReadsDoNotLeakThreadLocal() {
        for (int i = 0; i < 50; i++) {
            assertFalse(FrontendSecurityContext.isAuthenticated());
            assertFalse(FrontendSecurityContext.isAdmin());
            assertFalse(FrontendSecurityContext.hasRole(RoleEnum.ROLE_ADMIN));
            assertFalse(FrontendSecurityContext.hasRole(RoleEnum.ROLE_MARKETER));
            assertTrue(FrontendSecurityContext.getCurrentToken().isEmpty());
            assertTrue(FrontendSecurityContext.getCurrentUsername().isEmpty());
        }

        // Now set an explicit session and verify it works
        UserSession session = new UserSession(99L, "operator", "operator@crm.internal",
                RoleEnum.ROLE_MARKETER, "op-token");
        FrontendSecurityContext.setCurrentSession(session);
        assertTrue(FrontendSecurityContext.isAuthenticated());
        assertEquals("op-token", FrontendSecurityContext.getCurrentToken().orElse(null));

        // Explicit cleanup
        FrontendSecurityContext.clearThreadSession();
        assertFalse(FrontendSecurityContext.isAuthenticated());
        assertTrue(FrontendSecurityContext.getCurrentToken().isEmpty());
    }

    @Test
    @DisplayName("Active VaadinSession: Setting session does NOT contaminate worker thread fallback")
    void testVaadinSessionDoesNotContaminateWorkerThread() {
        VaadinSession mockSession = mock(VaadinSession.class);
        VaadinSession.setCurrent(mockSession);

        UserSession vaadinUserSession = new UserSession(10L, "vaadin_user", "v@crm.internal",
                RoleEnum.ROLE_ADMIN, "vaadin-jwt-token");

        // Set session within Vaadin request
        FrontendSecurityContext.setCurrentSession(vaadinUserSession);
        verify(mockSession).setAttribute(eq(UserSession.class), eq(vaadinUserSession));

        // Simulate end of Vaadin request: Vaadin unbinds VaadinSession from worker thread
        VaadinSession.setCurrent(null);

        // Worker thread must NOT retain fallbackSession
        assertFalse(FrontendSecurityContext.isAuthenticated(),
                "Worker thread returned to pool must not retain Vaadin session fallback");
        assertTrue(FrontendSecurityContext.getCurrentToken().isEmpty(),
                "Worker thread must not retain JWT token from Vaadin request");
    }

    @Test
    @DisplayName("Logout clears both VaadinSession and thread-local state")
    void testLogoutClearsAllState() {
        VaadinSession mockSession = mock(VaadinSession.class);
        UserSession userSession = new UserSession(1L, "admin", "admin@crm.internal", RoleEnum.ROLE_ADMIN, "secret-jwt");
        when(mockSession.getAttribute(UserSession.class)).thenReturn(userSession);
        VaadinSession.setCurrent(mockSession);

        FrontendSecurityContext.logout();

        verify(mockSession).setAttribute(UserSession.class, null);
        verify(mockSession).close();
        assertNull(userSession.getToken(), "UserSession.clear() should clear the token");

        // Clear VaadinSession and verify thread is also clean
        VaadinSession.setCurrent(null);
        assertFalse(FrontendSecurityContext.isAuthenticated());
        assertTrue(FrontendSecurityContext.getCurrentToken().isEmpty());
    }

    @Test
    @DisplayName("Nested scoped sessions restore outer session correctly")
    void testNestedScopedSessions() {
        UserSession outerSession = new UserSession(1L, "outer", "outer@crm.internal", RoleEnum.ROLE_MARKETER, "outer-jwt");
        UserSession innerSession = new UserSession(2L, "inner", "inner@crm.internal", RoleEnum.ROLE_ADMIN, "inner-jwt");

        FrontendSecurityContext.runWithSession(outerSession, () -> {
            assertEquals("outer-jwt", FrontendSecurityContext.getCurrentToken().orElse(null));
            assertFalse(FrontendSecurityContext.isAdmin());

            FrontendSecurityContext.runWithSession(innerSession, () -> {
                assertEquals("inner-jwt", FrontendSecurityContext.getCurrentToken().orElse(null));
                assertTrue(FrontendSecurityContext.isAdmin());
            });

            // Outer session restored
            assertEquals("outer-jwt", FrontendSecurityContext.getCurrentToken().orElse(null));
            assertFalse(FrontendSecurityContext.isAdmin());
        });

        // Completely clean after outer completes
        assertFalse(FrontendSecurityContext.isAuthenticated());
        assertTrue(FrontendSecurityContext.getCurrentToken().isEmpty());
    }
}
