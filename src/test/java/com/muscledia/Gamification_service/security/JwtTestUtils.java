package com.muscledia.Gamification_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Utility class for generating JWT tokens for testing purposes
 */
public class JwtTestUtils {

    private static final String TEST_SECRET = "mySecretKey12345678901234567890123456789012";
    private static final String ISSUER = "muscledia-user-service";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours

    /**
     * Generate a test JWT token for a user
     */
    public static String generateTestToken(Long userId, String username, String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", email)
                .claim("roles", roles)
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Generate a test token for a regular user
     */
    public static String generateUserToken(Long userId, String username) {
        return generateTestToken(userId, username, username + "@example.com", List.of("USER"));
    }

    /**
     * Generate a test token for an admin user
     */
    public static String generateAdminToken(Long userId, String username) {
        return generateTestToken(userId, username, username + "@example.com", List.of("USER", "ADMIN"));
    }

    /**
     * Generate an expired test token
     */
    public static String generateExpiredToken(Long userId, String username) {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 3600000); // 1 hour ago

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", username + "@example.com")
                .claim("roles", List.of("USER"))
                .issuer(ISSUER)
                .issuedAt(new Date(expiredDate.getTime() - 86400000)) // issued 24 hours before expiry
                .expiration(expiredDate)
                .signWith(key)
                .compact();
    }

    /**
     * Print sample tokens for manual testing
     */
    public static void main(String[] args) {
        System.out.println("=== JWT Test Tokens ===\n");

        // Regular user token
        String userToken = generateUserToken(123L, "testuser");
        System.out.println("Regular User Token (ID: 123, Username: testuser):");
        System.out.println("Bearer " + userToken);
        System.out.println();

        // Admin user token
        String adminToken = generateAdminToken(456L, "admin");
        System.out.println("Admin User Token (ID: 456, Username: admin):");
        System.out.println("Bearer " + adminToken);
        System.out.println();

        // Another user token for testing access control
        String user2Token = generateUserToken(789L, "user2");
        System.out.println("Another User Token (ID: 789, Username: user2):");
        System.out.println("Bearer " + user2Token);
        System.out.println();

        System.out.println("=== Usage Instructions ===");
        System.out.println("1. Copy one of the 'Bearer ...' tokens above");
        System.out.println("2. In Swagger UI or API client, add Authorization header:");
        System.out.println("   Authorization: Bearer <token>");
        System.out.println("3. Test different endpoints with different user tokens");
        System.out.println("4. Verify access control works correctly");
    }
}