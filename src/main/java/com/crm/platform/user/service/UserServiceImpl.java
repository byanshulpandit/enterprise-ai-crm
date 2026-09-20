package com.crm.platform.user.service;

import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("User id must not be null");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ResourceNotFoundException("Username must not be empty");
        }
        return userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username.trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ResourceNotFoundException("Email must not be empty");
        }
        return userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email.trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new ResourceNotFoundException("Identifier must not be empty");
        }
        String lookup = identifier.trim();
        return userRepository.findByUsernameOrEmail(lookup, lookup)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + lookup));
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countAdmins() {
        return userRepository.countByRole(RoleEnum.ROLE_ADMIN);
    }

    @Override
    public void validateActiveUser(User user) {
        if (user == null) {
            throw new ResourceNotFoundException("User must not be null");
        }
        if (!user.isActive()) {
            throw new InvalidRequestException("User account is inactive");
        }
    }
}
