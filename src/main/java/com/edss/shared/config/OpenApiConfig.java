package com.edss.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI edssOpenApi() {
        SecurityScheme bearer =
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT");
        return new OpenAPI()
                .info(
                        new Info()
                                .title("EDSS Business Operations Platform API")
                                .version("v1")
                                .description(
                                        "Modular monolith API. See modules under /api/v1. Error"
                                            + " responses follow the { code, message, details? }"
                                            + " envelope."))
                .components(new Components().addSecuritySchemes("bearerAuth", bearer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
