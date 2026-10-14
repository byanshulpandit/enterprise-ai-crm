package com.crm.platform.config;

import com.crm.platform.security.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Production Profile Configuration & Property Resolution Tests")
class ProductionConfigurationPropertiesTest {

    private ApplicationContextRunner createProdRunner() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withSystemProperties("spring.profiles.active=prod")
                .withUserConfiguration(ProductionSecurityEnvironmentValidator.class, JwtProperties.class);
    }

    @Test
    @DisplayName("Fail-fast: Prod profile fails startup when required DB credentials are absent")
    void shouldFailFastWhenDatabaseCredentialsMissing() {
        createProdRunner()
                .withPropertyValues(
                        "JWT_SECRET=super-secure-production-jwt-secret-key-at-least-32-chars-long!",
                        "REDIS_HOST=prod-redis.internal",
                        "SMTP_HOST=prod-smtp.internal",
                        "DB_HOST=prod-db.internal",
                        "DB_USERNAME=",
                        "DB_PASSWORD="
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure.getCause()).hasMessageContaining("spring.datasource.username");
                });
    }

    @Test
    @DisplayName("Fail-fast: Prod profile rejects default root user and requires least-privilege MySQL user")
    void shouldFailFastWhenRootUserConfiguredInProduction() {
        createProdRunner()
                .withPropertyValues(
                        "DB_HOST=prod-db.internal",
                        "DB_USERNAME=root",
                        "DB_PASSWORD=SomePassword123!",
                        "REDIS_HOST=prod-redis.internal",
                        "SMTP_HOST=prod-smtp.internal",
                        "JWT_SECRET=super-secure-production-jwt-secret-key-at-least-32-chars-long!"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure.getCause()).hasMessageContaining("Insecure database user 'root' is forbidden in production");
                });
    }

    @Test
    @DisplayName("Fail-fast: Prod profile rejects insecure JDBC connection without TLS")
    void shouldFailFastWhenTlsMissingInProductionJdbcUrl() {
        createProdRunner()
                .withPropertyValues(
                        "DB_HOST=prod-db.internal",
                        "SPRING_DATASOURCE_URL=jdbc:mysql://prod-db.internal:3306/crm_db?useSSL=false",
                        "DB_USERNAME=crm_app_user",
                        "DB_PASSWORD=SomePassword123!",
                        "REDIS_HOST=prod-redis.internal",
                        "SMTP_HOST=prod-smtp.internal",
                        "JWT_SECRET=super-secure-production-jwt-secret-key-at-least-32-chars-long!"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure.getCause()).hasMessageContaining("TLS is required for production database connections");
                });
    }

    @Test
    @DisplayName("Fail-fast: Prod profile fails startup when JWT_SECRET is absent")
    void shouldFailFastWhenJwtSecretMissing() {
        createProdRunner()
                .withPropertyValues(
                        "DB_HOST=prod-db.internal",
                        "DB_USERNAME=crm_least_privilege_user",
                        "DB_PASSWORD=SecureProductionPassword2026!",
                        "REDIS_HOST=prod-redis.internal",
                        "SMTP_HOST=prod-smtp.internal",
                        "JWT_SECRET="
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure).isNotNull();
                    assertThat(failure.getCause()).hasMessageContaining("security.jwt.secret / JWT_SECRET is mandatory");
                });
    }

    @Test
    @DisplayName("Prod profile resolves securely: enforces TLS, least-privilege user, and no dev defaults")
    void shouldResolveProductionPropertiesSecurely() {
        createProdRunner()
                .withPropertyValues(
                        "DB_HOST=prod-db.internal",
                        "DB_PORT=3306",
                        "DB_NAME=crm_production",
                        "DB_USERNAME=crm_app_user",
                        "DB_PASSWORD=VeryStrongProdPassword123#",
                        "REDIS_HOST=prod-redis.internal",
                        "REDIS_PORT=6379",
                        "REDIS_PASSWORD=RedisProdSecret2026!",
                        "SMTP_HOST=prod-smtp.sendgrid.net",
                        "SMTP_PORT=587",
                        "SMTP_USERNAME=smtp_api_user",
                        "SMTP_PASSWORD=SmtpSecretKey123!",
                        "SMTP_FROM=notifications@company.com",
                        "JWT_SECRET=production-grade-hmac-sha256-signing-secret-key-2026-secure",
                        "GEMINI_API_KEY=AIzaSyProductionGeminiKey-12345"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Environment env = context.getEnvironment();

                    // Verify DataSource settings enforce TLS and deny public key retrieval
                    String dsUrl = env.getProperty("spring.datasource.url");
                    assertThat(dsUrl).isNotNull();
                    assertThat(dsUrl).contains("sslMode=REQUIRED");
                    assertThat(dsUrl).contains("allowPublicKeyRetrieval=false");
                    assertThat(dsUrl).contains("prod-db.internal:3306/crm_production");

                    // Verify DB credentials are the configured least-privilege user, NOT root
                    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("crm_app_user");
                    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("VeryStrongProdPassword123#");

                    // Verify SQL init mode is never in production
                    assertThat(env.getProperty("spring.sql.init.mode")).isEqualTo("never");

                    // Verify Redis bindings
                    assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("prod-redis.internal");
                    assertThat(env.getProperty("spring.data.redis.port")).isEqualTo("6379");
                    assertThat(env.getProperty("spring.data.redis.password")).isEqualTo("RedisProdSecret2026!");

                    // Verify SMTP bindings
                    assertThat(env.getProperty("smtp.host")).isEqualTo("prod-smtp.sendgrid.net");
                    assertThat(env.getProperty("smtp.port")).isEqualTo("587");
                    assertThat(env.getProperty("smtp.username")).isEqualTo("smtp_api_user");
                    assertThat(env.getProperty("smtp.password")).isEqualTo("SmtpSecretKey123!");
                    assertThat(env.getProperty("smtp.from")).isEqualTo("notifications@company.com");

                    // Verify Delivery Provider defaults to smtp in prod
                    assertThat(env.getProperty("crm.delivery.provider")).isEqualTo("smtp");

                    // Verify Gemini bindings
                    assertThat(env.getProperty("crm.ai.gemini.api-key")).isEqualTo("AIzaSyProductionGeminiKey-12345");

                    // Verify JWT Bean loaded and validated
                    JwtProperties jwtProperties = context.getBean(JwtProperties.class);
                    assertThat(jwtProperties.getSecret()).isEqualTo("production-grade-hmac-sha256-signing-secret-key-2026-secure");
                    assertThat(jwtProperties.getIssuer()).isEqualTo("cs-crm-2026");
                });
    }

    @Test
    @DisplayName("Alternative Spring standard env variable bindings are resolved correctly in prod")
    void shouldResolveSpringStandardEnvironmentVariables() {
        createProdRunner()
                .withPropertyValues(
                        "DB_HOST=custom-cluster.internal",
                        "SPRING_DATASOURCE_URL=jdbc:mysql://custom-cluster.internal:3306/crm_cluster?sslMode=REQUIRED&allowPublicKeyRetrieval=false",
                        "DB_USERNAME=cluster_user",
                        "DB_PASSWORD=ClusterPassword999!",
                        "REDIS_HOST=redis-cluster.internal",
                        "SMTP_HOST=mail-relay.internal",
                        "JWT_SECRET=cluster-level-hmac-sha256-secret-must-be-very-long-and-secure",
                        "CRM_DELIVERY_PROVIDER=simulated"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Environment env = context.getEnvironment();

                    assertThat(env.getProperty("spring.datasource.url")).isEqualTo(
                            "jdbc:mysql://custom-cluster.internal:3306/crm_cluster?sslMode=REQUIRED&allowPublicKeyRetrieval=false");
                    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("cluster_user");
                    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("ClusterPassword999!");
                    assertThat(env.getProperty("spring.data.redis.host")).isEqualTo("redis-cluster.internal");
                    assertThat(env.getProperty("smtp.host")).isEqualTo("mail-relay.internal");
                    assertThat(env.getProperty("crm.delivery.provider")).isEqualTo("simulated");
                });
    }
}
