package com.muscledia.Gamification_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Utility class for generating JWT tokens in tests.
 * Provides methods to create valid, expired, and role-specific tokens.
 */
public class JwtTestUtils {

    private static final String TEST_SECRET = "testSecretKey123456789012345678901234567890";
    private static final String TEST_ISSUER = "muscledia-user-service-test";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

    /**
     * Generates a valid JWT token for a user with USER role.
     */
    public static String generateUserToken(Long userId, String username) {
        return generateToken(userId, username, "user@example.com", List.of("USER"), 24);
    }

    /**
     * Generates a valid JWT token for an admin with ADMIN and USER roles.
     */
    public static String generateAdminToken(Long userId, String username) {
        return generateToken(userId, username, "admin@example.com", List.of("ADMIN", "USER"), 24);
    }

    /**
     * Generates an expired JWT token for testing expired token scenarios.
     */
    public static String generateExpiredToken(Long userId, String username) {
        return generateToken(userId, username, "expired@example.com", List.of("USER"), -1);
    }

    /**
     * Generates a JWT token with custom roles.
     */
    public static String generateTokenWithRoles(Long userId, String username, List<String> roles) {
        return generateToken(userId, username, "custom@example.com", roles, 24);
    }

    /**
     * Generates a JWT token with custom expiration hours.
     */
    public static String generateTokenWithExpiration(Long userId, String username, int expirationHours) {
        return generateToken(userId, username, "user@example.com", List.of("USER"), expirationHours);
    }

    /**
     * Core method to generate JWT tokens with all customizable parameters.
     */
    private static String generateToken(Long userId, String username, String email,
            List<String> roles, int expirationHours) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", email)
                .claim("roles", roles)
                .setIssuer(TEST_ISSUER)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Generates a token with invalid signature for security testing.
     */
    public static String generateInvalidSignatureToken(Long userId, String username) {
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrongSecretKey12345678901234567890123".getBytes());
        Instant now = Instant.now();
        Instant expiration = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", "test@example.com")
                .claim("roles", List.of("USER"))
                .setIssuer(TEST_ISSUER)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(wrongKey) // Wrong signature
                .compact();
    }

    /**
     * Generates a token with missing required claims for testing validation.
     */
    public static String generateIncompleteToken(String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .setSubject(username)
                // Missing userId claim intentionally
                .claim("username", username)
                .setIssuer(TEST_ISSUER)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Generates a token with wrong issuer for testing issuer validation.
     */
    public static String generateWrongIssuerToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", "test@example.com")
                .claim("roles", List.of("USER"))
                .setIssuer("wrong-issuer") // Wrong issuer
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Creates a Bearer token string ready for Authorization header.
     */
    public static String toBearerToken(String token) {
        return "Bearer " + token;
    }

    /**
     * Utility method to get the test secret key for other test configurations.
     */
    public static String getTestSecret() {
        return TEST_SECRET;
    }

    /**
     * Utility method to get the test issuer for other test configurations.
     */
    public static String getTestIssuer() {
        return TEST_ISSUER;
    }
}