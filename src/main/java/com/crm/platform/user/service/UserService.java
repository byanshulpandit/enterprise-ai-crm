package com.crm.platform.user.service;

import com.crm.platform.user.dto.UserCreateRequest;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User findById(Long id);

    User findByUsername(String username);

    User findByEmail(String email);

    User findByUsernameOrEmail(String identifier);

    long countUsers();

    long countAdmins();

    void validateActiveUser(User user);

    User createUser(UserCreateRequest request);

    Page<User> listUsers(Pageable pageable);

    User updateUserRole(Long id, RoleEnum newRole, String callerUsername);

    User deactivateUser(Long id, String callerUsername);

    void updateUserPassword(Long id, String newPassword);
}
