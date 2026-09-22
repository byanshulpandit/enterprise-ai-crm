package com.crm.platform.user.service;

import com.crm.platform.common.exception.DuplicateResourceException;
import com.crm.platform.common.exception.InvalidRequestException;
import com.crm.platform.common.exception.ResourceNotFoundException;
import com.crm.platform.security.PasswordValidator;
import com.crm.platform.user.dto.UserCreateRequest;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Override
    public User createUser(UserCreateRequest request) {
        if (request == null) {
            throw new InvalidRequestException("User creation request must not be null");
        }
        PasswordValidator.validate(request.getPassword());

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }
        if (request.getRole() == null) {
            throw new InvalidRequestException("Role is required and must be ROLE_ADMIN or ROLE_MARKETER");
        }

        String passwordHash = passwordEncoder != null ? passwordEncoder.encode(request.getPassword()) : request.getPassword();
        User user = new User(username, email, passwordHash, request.getRole(), Boolean.TRUE);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public User updateUserRole(Long id, RoleEnum newRole, String callerUsername) {
        if (newRole == null) {
            throw new InvalidRequestException("Role is required and must be ROLE_ADMIN or ROLE_MARKETER");
        }
        User user = findById(id);

        if (callerUsername != null && user.getUsername().equalsIgnoreCase(callerUsername) && newRole != RoleEnum.ROLE_ADMIN) {
            throw new InvalidRequestException("Administrators cannot demote their own account");
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Override
    public User deactivateUser(Long id, String callerUsername) {
        User user = findById(id);

        if (callerUsername != null && user.getUsername().equalsIgnoreCase(callerUsername)) {
            throw new InvalidRequestException("Administrators cannot deactivate their own account");
        }

        user.setIsActive(false);
        return userRepository.save(user);
    }

    @Override
    public void updateUserPassword(Long id, String newPassword) {
        PasswordValidator.validate(newPassword);
        User user = findById(id);

        String passwordHash = passwordEncoder != null ? passwordEncoder.encode(newPassword) : newPassword;
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
    }
}
