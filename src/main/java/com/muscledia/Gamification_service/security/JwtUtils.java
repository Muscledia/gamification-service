package com.muscledia.Gamification_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // Default 24 hours
    private int jwtExpirationMs;

    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * Get signing key from the secret
     */
    private SecretKey getSigningKey() {
        // Compatible with User Service - handles both base64 and raw secret
        try {
            // Try base64 decoding first (as your User Service does)
            return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        } catch (IllegalArgumentException e) {
            // Fallback to raw bytes if not base64
            log.debug("Secret is not base64 encoded, using raw bytes");
            return Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateJwtToken(String authToken) {

        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    //.requireIssuer(expectedIssuer)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract user ID from JWT token
     */
    public Long getUserIdFromJwtToken(String token) {
        Claims claims = extractClaims(token);

        // Try to get userIdLong first (if available)
        Object userIdLong = claims.get("userIdLong");
        if (userIdLong instanceof Long) {
            return (Long) userIdLong;
        }
        if (userIdLong instanceof Integer) {
            return ((Integer) userIdLong).longValue();
        }

        // Fallback to userId as String and convert
        Object userId = claims.get("userId");
        if (userId instanceof String) {
            try {
                return Long.valueOf((String) userId);
            } catch (NumberFormatException e) {
                log.error("Failed to convert userId string to Long: {}", userId);
                return null;
            }
        }
        if (userId instanceof Long) {
            return (Long) userId;
        }
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }

        log.warn("Could not extract userId from token. Available claims: {}", claims.keySet());
        return null;
    }


    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * Extract username from JWT token
     */
    public String getUsernameFromJwtToken(String token) {
        return extractClaims(token).getSubject();
    }


    /**
     * Extract roles from JWT token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromJwtToken(String token) {
        Claims claims = extractClaims(token);

        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List) {
            return (List<String>) rolesClaim;
        }
        // Fallback to single role if available
        String singleRole = claims.get("role", String.class);
        if (singleRole != null) {
            return List.of(singleRole);
        }

        return Collections.emptyList();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Extract all claims from JWT token
     */
    public Claims getClaimsFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get token expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromJwtToken(token);
        return claims.getExpiration();
    }

    /**
     * Get token issued date
     */
    public Date getIssuedAtDateFromToken(String token) {
        Claims claims = getClaimsFromJwtToken(token);
        return claims.getIssuedAt();
    }
}