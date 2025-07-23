package com.muscledia.Gamification_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for simplified testing setup.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    // Simple test configuration without Testcontainers
    // Uses default embedded MongoDB configuration
}