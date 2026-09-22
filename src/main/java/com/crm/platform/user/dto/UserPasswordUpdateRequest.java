package com.crm.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UserPasswordUpdateRequest {

    @NotBlank(message = "Password is required")
    private String password;

    public UserPasswordUpdateRequest() {}

    public UserPasswordUpdateRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
