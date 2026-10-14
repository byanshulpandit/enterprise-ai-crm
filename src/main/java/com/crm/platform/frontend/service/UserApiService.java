package com.crm.platform.frontend.service;

import com.crm.platform.frontend.client.ApiEndpointConstants;
import com.crm.platform.frontend.client.BackendApiClient;
import com.crm.platform.frontend.client.PageResult;
import com.crm.platform.user.dto.UserCreateRequest;
import com.crm.platform.user.dto.UserPasswordUpdateRequest;
import com.crm.platform.user.dto.UserResponse;
import com.crm.platform.user.dto.UserRoleUpdateRequest;
import com.crm.platform.user.entity.RoleEnum;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service invoking backend User Management REST endpoints (ADMIN restricted).
 */
@Service
public class UserApiService {

    private final BackendApiClient apiClient;

    public UserApiService(BackendApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public UserResponse createUser(UserCreateRequest request) {
        return apiClient.post(ApiEndpointConstants.USERS, request, UserResponse.class);
    }

    public PageResult<UserResponse> listUsers(int page, int size) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("page", page);
        queryParams.put("size", size);
        return apiClient.getPage(ApiEndpointConstants.USERS, UserResponse.class, queryParams);
    }

    public UserResponse getUserById(Long id) {
        return apiClient.get(ApiEndpointConstants.USER_BY_ID, UserResponse.class, id);
    }

    public UserResponse updateUserRole(Long id, RoleEnum role) {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(role);
        return apiClient.patch(ApiEndpointConstants.USER_ROLE, request, UserResponse.class, id);
    }

    public UserResponse deactivateUser(Long id) {
        return apiClient.patch(ApiEndpointConstants.USERS + "/" + id + "/deactivate", null, UserResponse.class);
    }

    public void updateUserPassword(Long id, String password) {
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(password);
        apiClient.patch(ApiEndpointConstants.USER_PASSWORD, request, Map.class, id);
    }
}
