package com.crm.platform.user.service;

import com.crm.platform.user.entity.User;

public interface UserService {

    User findById(Long id);

    User findByUsername(String username);

    User findByEmail(String email);

    User findByUsernameOrEmail(String identifier);

    long countUsers();

    long countAdmins();

    void validateActiveUser(User user);
}
