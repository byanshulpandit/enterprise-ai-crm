package com.crm.platform.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private UserSummary user;

    public LoginResponse() {}

    public LoginResponse(String token, String tokenType, UserSummary user) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public static class UserSummary {
        private Long id;
        private String username;
        private String role;

        public UserSummary() {}

        public UserSummary(Long id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
