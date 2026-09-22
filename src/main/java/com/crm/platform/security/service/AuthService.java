package com.crm.platform.security.service;

import com.crm.platform.security.dto.LoginRequest;
import com.crm.platform.security.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
