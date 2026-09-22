package com.crm.platform.security.bootstrap;

import com.crm.platform.security.PasswordValidator;
import com.crm.platform.user.entity.RoleEnum;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties bootstrapProperties;

    public AdminBootstrapRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                AdminBootstrapProperties bootstrapProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapProperties = bootstrapProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long userCount = userRepository.count();
        if (userCount > 0) {
            log.info("Users table is not empty (count: {}). Initial admin bootstrap skipped.", userCount);
            return;
        }

        log.info("Users table is empty. Initiating initial admin bootstrap...");

        if (bootstrapProperties == null
                || !StringUtils.hasText(bootstrapProperties.getUsername())
                || !StringUtils.hasText(bootstrapProperties.getEmail())
                || !StringUtils.hasText(bootstrapProperties.getPassword())) {
            throw new IllegalStateException(
                    "Initial admin bootstrap required (users table is empty), but mandatory bootstrap configuration " +
                    "(security.bootstrap.admin.username, email, password) is missing."
            );
        }

        String username = bootstrapProperties.getUsername().trim();
        String email = bootstrapProperties.getEmail().trim().toLowerCase();
        String rawPassword = bootstrapProperties.getPassword();

        PasswordValidator.validate(rawPassword);

        String passwordHash = passwordEncoder.encode(rawPassword);
        User admin = new User(username, email, passwordHash, RoleEnum.ROLE_ADMIN, Boolean.TRUE);
        userRepository.save(admin);

        log.info("Initial admin user '{}' successfully bootstrapped with ROLE_ADMIN.", admin.getUsername());
    }
}
