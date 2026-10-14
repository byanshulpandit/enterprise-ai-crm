package com.crm.platform.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Frontend application.
 */
@Configuration
@ConfigurationProperties(prefix = "crm.frontend")
public class FrontendProperties {

    /**
     * Base URL of the backend REST API.
     * Defaults to http://localhost:8080 if not explicitly specified.
     */
    private String apiBaseUrl = "http://localhost:8080";

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
