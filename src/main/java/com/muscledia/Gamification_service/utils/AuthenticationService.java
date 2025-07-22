package com.muscledia.Gamification_service.utils;

import com.muscledia.Gamification_service.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationService {

    /**
     * Get the currently authenticated user
     */
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }

        throw new SecurityException("No authenticated user found");
    }

    /**
     * Get the current user's ID
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * Get the current user's username
     */
    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    /**
     * Check if the current user has a specific role
     */
    public static boolean hasRole(String role) {
        try {
            return getCurrentUser().hasRole(role);
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Check if the current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        try {
            return getCurrentUser().hasAnyRole(roles);
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Check if the current user is an admin
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if the current user is the owner of the resource or an admin
     */
    public static boolean isOwnerOrAdmin(Long resourceUserId) {
        try {
            Long currentUserId = getCurrentUserId();
            return currentUserId.equals(resourceUserId) || isAdmin();
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Validate that the current user can access a resource belonging to the
     * specified user
     */
    public static void validateUserAccess(Long targetUserId) {
        if (!isOwnerOrAdmin(targetUserId)) {
            throw new SecurityException("Access denied: You can only access your own resources");
        }
    }

    /**
     * Check if there is an authenticated user
     */
    public static boolean isAuthenticated() {
        try {
            getCurrentUser();
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}