package com.muscledia.Gamification_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Muscledia Gamification Service API")
                                                .description("REST API for managing gamification logic, badges, quests, championships, and user progress tracking in the Muscledia platform.")
                                                .version("v1.0.0")
                                                .contact(new Contact()
                                                                .name("Muscledia Development Team")
                                                                .email("dev@muscledia.com"))
                                                .license(new License()
                                                                .name("MIT License")
                                                                .url("https://opensource.org/licenses/MIT")))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("JWT token obtained from muscledia-user-service. "
                                                                                +
                                                                                "Format: Bearer {your-jwt-token}")))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        }
}