package com.crm.platform.frontend.security;

import com.crm.platform.user.entity.RoleEnum;
import java.io.Serializable;

/**
 * Encapsulates the authenticated user's session state in VaadinSession.
 * Holds user identification, role authorities, and the 1-hour JWT token.
 */
public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private RoleEnum role;
    private String token;

    public UserSession() {
    }

    public UserSession(Long id, String username, String email, RoleEnum role, String token) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }

    public boolean isAdmin() {
        return role == RoleEnum.ROLE_ADMIN;
    }

    public boolean isMarketer() {
        return role == RoleEnum.ROLE_MARKETER;
    }

    public boolean hasRole(RoleEnum requiredRole) {
        if (requiredRole == null) {
            return true;
        }
        if (role == RoleEnum.ROLE_ADMIN) {
            // ADMIN inherits MARKETER capabilities
            return true;
        }
        return role == requiredRole;
    }

    public void clear() {
        this.id = null;
        this.username = null;
        this.email = null;
        this.role = null;
        this.token = null;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
