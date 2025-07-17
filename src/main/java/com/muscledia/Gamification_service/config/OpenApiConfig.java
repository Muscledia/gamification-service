package com.muscledia.Gamification_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Muscledia Gamification Service API")
                        .description(
                                "REST API for the Muscledia gamification microservice providing comprehensive badge, quest, champion, and user profile management for fitness applications.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Muscledia Development Team")
                                .email("dev@muscledia.com")
                                .url("https://muscledia.com"))
                );

    }
}