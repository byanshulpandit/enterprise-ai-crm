package com.crm.platform.frontend.service;

import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.security.FrontendSecurityContext;
import com.crm.platform.frontend.security.UserSession;
import com.crm.platform.security.dto.LoginRequest;
import com.crm.platform.security.dto.LoginResponse;
import com.crm.platform.user.entity.RoleEnum;
import org.springframework.stereotype.Service;

/**
 * Service managing user authentication, login and logout workflows.
 */
@Service
public class AuthApiService {

    private final BackendApiClient apiClient;

    public AuthApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public UserSession login(String usernameOrEmail, String password) {
        LoginRequest request = new LoginRequest(usernameOrEmail, password);
        LoginResponse response = apiClient.post(ApiEndpointConstants.AUTH_LOGIN, request, LoginResponse.class);

        if (response != null && response.getToken() != null) {
            RoleEnum role = RoleEnum.ROLE_MARKETER;
            if (response.getUser() != null && response.getUser().getRole() != null) {
                try {
                    role = RoleEnum.valueOf(response.getUser().getRole());
                } catch (IllegalArgumentException ignored) {
                    role = RoleEnum.ROLE_MARKETER;
                }
            }

            Long userId = response.getUser() != null ? response.getUser().getId() : null;
            String username = response.getUser() != null ? response.getUser().getUsername() : usernameOrEmail;

            UserSession session = new UserSession(userId, username, usernameOrEmail, role, response.getToken());
            FrontendSecurityContext.setCurrentSession(session);
            return session;
        }

        throw new IllegalStateException("Authentication failed: invalid token received");
    }

    public void logout() {
        FrontendSecurityContext.logout();
    }
}
