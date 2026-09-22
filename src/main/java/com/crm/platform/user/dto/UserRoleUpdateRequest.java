package com.crm.platform.user.dto;

import com.crm.platform.user.entity.RoleEnum;
import jakarta.validation.constraints.NotNull;

public class UserRoleUpdateRequest {

    @NotNull(message = "Role is required")
    private RoleEnum role;

    public UserRoleUpdateRequest() {}

    public UserRoleUpdateRequest(RoleEnum role) {
        this.role = role;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }
}
