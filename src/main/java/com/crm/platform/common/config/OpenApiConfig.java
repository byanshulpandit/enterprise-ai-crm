package com.crm.platform.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise AI-CRM Platform API")
                        .version("1.0.0")
                        .description("Production-grade Enterprise AI-CRM Platform REST API. " +
                                "Provides JWT Bearer Authentication, Customer Lifecycle Management, " +
                                "Bulk CSV/XLSX Ingestion, Dynamic Boolean AST Segmentation, " +
                                "Redis Streams Asynchronous Campaign Delivery Simulation, " +
                                "Google Gemini AI Rule Translation, Campaign Narrative Summaries, " +
                                "and Real-time Analytics Reporting.")
                        .contact(new Contact().name("Enterprise CRM Team").email("engineering@crm.internal"))
                        .license(new License().name("Proprietary").url("https://crm.internal/license")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (obtained via POST /api/v1/auth/login)")));
    }
}
