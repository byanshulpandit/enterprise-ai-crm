package com.crm.platform.security;

import com.crm.platform.common.exception.InvalidRequestException;

import java.nio.charset.StandardCharsets;

public final class PasswordValidator {

    private PasswordValidator() {}

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new InvalidRequestException("Password must be between 8 and 72 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("Password must not exceed 72 UTF-8 bytes");
        }
    }
}
