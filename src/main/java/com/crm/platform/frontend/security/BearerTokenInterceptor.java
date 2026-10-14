package com.crm.platform.frontend.security;

import com.crm.platform.frontend.util.NotificationUtils;
import com.vaadin.flow.component.UI;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Injects JWT Bearer token into outgoing REST requests and handles HTTP 401 Session Expiration.
 */
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        FrontendSecurityContext.getCurrentToken().ifPresent(token -> {
            if (!token.isBlank()) {
                request.getHeaders().setBearerAuth(token);
            }
        });

        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            String path = request.getURI().getPath();
            boolean isLoginEndpoint = path != null && path.contains("/api/v1/auth/login");
            if (!isLoginEndpoint) {
                FrontendSecurityContext.logout();
                UI currentUi = UI.getCurrent();
                if (currentUi != null) {
                    currentUi.access(() -> {
                        NotificationUtils.showError("Session expired. Please log in again.");
                        currentUi.navigate("login");
                    });
                }
            }
        }

        return response;
    }
}
