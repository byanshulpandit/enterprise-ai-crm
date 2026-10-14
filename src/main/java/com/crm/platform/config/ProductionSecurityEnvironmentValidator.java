package com.crm.platform.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * Validates production environment properties on startup, ensuring fail-fast enforcement
 * for mandatory secrets, least-privilege database user, TLS enforcement, and disabled dev defaults.
 */
@Component
@Profile("prod")
public class ProductionSecurityEnvironmentValidator {

    private final Environment environment;

    public ProductionSecurityEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateProductionConfiguration() {
        // 1. Validate Database credentials and least-privilege user
        String dbUser = environment.getProperty("spring.datasource.username");
        if (!StringUtils.hasText(dbUser)) {
            throw new IllegalStateException("Production startup failed: spring.datasource.username / DB_USERNAME is mandatory and must not be blank.");
        }
        if ("root".equalsIgnoreCase(dbUser.trim())) {
            throw new IllegalStateException("Production startup failed: Insecure database user 'root' is forbidden in production profile. A least-privilege MySQL user must be configured.");
        }

        String dbPassword = environment.getProperty("spring.datasource.password");
        if (!StringUtils.hasText(dbPassword)) {
            throw new IllegalStateException("Production startup failed: spring.datasource.password / DB_PASSWORD is mandatory and must not be empty in production profile.");
        }

        // 2. Validate Database URL TLS and security options
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (!StringUtils.hasText(dbUrl)) {
            throw new IllegalStateException("Production startup failed: spring.datasource.url / DB_HOST is mandatory in production profile.");
        }
        if (!dbUrl.contains("sslMode=REQUIRED") && !dbUrl.contains("useSSL=true")) {
            throw new IllegalStateException("Production startup failed: TLS is required for production database connections (missing sslMode=REQUIRED).");
        }
        if (dbUrl.contains("allowPublicKeyRetrieval=true")) {
            throw new IllegalStateException("Production startup failed: Insecure allowPublicKeyRetrieval=true is forbidden in production profile.");
        }

        // 3. Validate SQL init mode
        String sqlInitMode = environment.getProperty("spring.sql.init.mode");
        if ("always".equalsIgnoreCase(sqlInitMode)) {
            throw new IllegalStateException("Production startup failed: spring.sql.init.mode must not be 'always' in production profile.");
        }

        // 4. Validate Redis host
        String redisHost = environment.getProperty("spring.data.redis.host");
        if (!StringUtils.hasText(redisHost)) {
            throw new IllegalStateException("Production startup failed: spring.data.redis.host / REDIS_HOST is mandatory in production profile.");
        }

        // 5. Validate SMTP host
        String smtpHost = environment.getProperty("smtp.host");
        if (!StringUtils.hasText(smtpHost)) {
            throw new IllegalStateException("Production startup failed: smtp.host / SMTP_HOST is mandatory in production profile.");
        }

        // 6. Validate JWT Secret
        String jwtSecret = environment.getProperty("security.jwt.secret");
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("Production startup failed: security.jwt.secret / JWT_SECRET is mandatory in production profile.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Production startup failed: security.jwt.secret must be at least 32 bytes (256 bits) in production.");
        }
    }
}
