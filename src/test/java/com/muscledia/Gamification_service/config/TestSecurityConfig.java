package com.muscledia.Gamification_service.config;

import com.muscledia.Gamification_service.security.UserPrincipal;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Test security configuration for unit and integration tests.
 * Provides simplified security setup and test utilities.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Disabled security configuration for unit tests.
     * Allows tests to run without authentication.
     */
    @Bean
    @Primary
    @Profile("test")
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    /**
     * Creates a test user principal for testing authenticated scenarios.
     */
    public static UserPrincipal createTestUser() {
        return createTestUser(12345L, "testuser", List.of("USER"));
    }

    /**
     * Creates a test admin principal for testing admin scenarios.
     */
    public static UserPrincipal createTestAdmin() {
        return createTestUser(99999L, "admin", List.of("ADMIN", "USER"));
    }

    /**
     * Creates a custom test user principal with specified details.
     */
    public static UserPrincipal createTestUser(Long userId, String username, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        return new UserPrincipal(userId, username, authorities);
    }
}