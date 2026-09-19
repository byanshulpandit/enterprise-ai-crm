package com.crm.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlatformApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private DataSource dataSource;

    @Test
    @DisplayName("Verify Spring Boot application context loads successfully")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("Verify DataSource bean configuration in application context")
    void dataSourceBeanConfigured() {
        assertThat(dataSource).isNotNull();
    }
}
