package com.crm.platform.security.service;

import com.crm.platform.security.JwtTokenProvider;
import com.crm.platform.security.dto.LoginRequest;
import com.crm.platform.security.dto.LoginResponse;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String lookup = request.getUsername().trim();
        User user = userRepository.findByUsernameOrEmail(lookup, lookup)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                new LoginResponse.UserSummary(user.getId(), user.getUsername(), user.getRole().name())
        );
    }
}
