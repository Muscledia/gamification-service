package com.muscledia.Gamification_service.utils;

import com.muscledia.Gamification_service.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationService {

    /**
     * Get the currently authenticated user
     */
    public static UserPrincipal getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                log.warn("No authentication found in security context");
                throw new AccessDeniedException("No authentication found");
            }

            if (!authentication.isAuthenticated()) {
                log.warn("User is not authenticated");
                throw new AccessDeniedException("User is not authenticated");
            }

            if (!(authentication.getPrincipal() instanceof UserPrincipal)) {
                log.warn("Authentication principal is not UserPrincipal: {}",
                        authentication.getPrincipal().getClass().getSimpleName());
                throw new AccessDeniedException("Invalid authentication principal");
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // Validate that the user principal has required information
            if (userPrincipal.getUserId() == null) {
                log.warn("UserPrincipal has null userId");
                throw new AccessDeniedException("Invalid user principal - missing userId");
            }

            return userPrincipal;

        } catch (AccessDeniedException e) {
            throw e; // Re-throw access denied exceptions
        } catch (Exception e) {
            log.error("Unexpected error getting current user: {}", e.getMessage(), e);
            throw new AccessDeniedException("Authentication error: " + e.getMessage());
        }
    }

    /**
     * Get the current user's ID
     */
    public static Long getCurrentUserId() {
        try {
            return getCurrentUser().getUserId();
        } catch (Exception e) {
            log.error("Error getting current user ID: {}", e.getMessage());
            throw new AccessDeniedException("Cannot determine current user ID");
        }
    }

    /**
     * Get the current user's username
     */
    public static String getCurrentUsername() {
        try {
            UserPrincipal user = getCurrentUser();
            String username = user.getUsername();
            return username != null ? username : "unknown";
        } catch (Exception e) {
            log.error("Error getting current username: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Get the current user's ID safely (returns null if not authenticated)
     */
    public static Long getCurrentUserIdSafe() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            log.debug("Could not get current user ID safely: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the current user's username safely (returns "anonymous" if not authenticated)
     */
    public static String getCurrentUsernameSafe() {
        try {
            return getCurrentUsername();
        } catch (Exception e) {
            log.debug("Could not get current username safely: {}", e.getMessage());
            return "anonymous";
        }
    }

    /**
     * Check if the current user has a specific role
     */
    public static boolean hasRole(String role) {
        try {
            UserPrincipal user = getCurrentUser();
            boolean hasRole = user.hasRole(role);
            log.debug("User {} has role {}: {}", user.getUsername(), role, hasRole);
            return hasRole;
        } catch (Exception e) {
            log.debug("Error checking role '{}': {}", role, e.getMessage());
            return false;
        }
    }

    /**
     * Check if the current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            log.warn("No roles provided to check");
            return false;
        }

        try {
            UserPrincipal user = getCurrentUser();
            boolean hasAnyRole = user.hasAnyRole(roles);
            log.debug("User {} has any of roles {}: {}", user.getUsername(),
                    String.join(", ", roles), hasAnyRole);
            return hasAnyRole;
        } catch (Exception e) {
            log.debug("Error checking roles {}: {}", String.join(", ", roles), e.getMessage());
            return false;
        }
    }

    /**
     * Check if the current user has all of the specified roles
     */
    public static boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return true; // No roles required
        }

        try {
            UserPrincipal user = getCurrentUser();
            for (String role : roles) {
                if (!user.hasRole(role)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("Error checking all roles {}: {}", String.join(", ", roles), e.getMessage());
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
        if (resourceUserId == null) {
            log.warn("Cannot check ownership for null resourceUserId");
            return false;
        }

        try {
            Long currentUserId = getCurrentUserId();
            boolean isOwner = currentUserId.equals(resourceUserId);
            boolean isAdmin = isAdmin();
            boolean hasAccess = isOwner || isAdmin;

            log.debug("Access check for user {} to resource owned by {}: owner={}, admin={}, access={}",
                    currentUserId, resourceUserId, isOwner, isAdmin, hasAccess);

            return hasAccess;
        } catch (Exception e) {
            log.debug("Error checking owner or admin for resource {}: {}", resourceUserId, e.getMessage());
            return false;
        }
    }

    /**
     * Validate that the current user can access a resource belonging to the specified user
     */
    public static void validateUserAccess(Long targetUserId) {
        if (targetUserId == null) {
            throw new AccessDeniedException("Target user ID cannot be null");
        }

        try {
            if (!isOwnerOrAdmin(targetUserId)) {
                Long currentUserId = getCurrentUserIdSafe();
                String currentUsername = getCurrentUsernameSafe();

                log.warn("Access denied: User {} (ID: {}) attempted to access resources for user ID {}",
                        currentUsername, currentUserId, targetUserId);

                throw new AccessDeniedException(
                        String.format("Access denied: You can only access your own resources. " +
                                "Current user: %d, Requested user: %d", currentUserId, targetUserId));
            }

            log.debug("Access granted for user {} to access resources for user {}",
                    getCurrentUserIdSafe(), targetUserId);

        } catch (AccessDeniedException e) {
            throw e; // Re-throw access denied exceptions
        } catch (Exception e) {
            log.error("Unexpected error during access validation: {}", e.getMessage(), e);
            throw new AccessDeniedException("Access validation failed: " + e.getMessage());
        }
    }

    /**
     * Validate that the current user has a specific role
     */
    public static void validateRole(String role) {
        if (!hasRole(role)) {
            String currentUsername = getCurrentUsernameSafe();
            log.warn("Access denied: User {} does not have required role: {}", currentUsername, role);
            throw new AccessDeniedException("Access denied: Required role '" + role + "' not found");
        }
    }

    /**
     * Validate that the current user has any of the specified roles
     */
    public static void validateAnyRole(String... roles) {
        if (!hasAnyRole(roles)) {
            String currentUsername = getCurrentUsernameSafe();
            log.warn("Access denied: User {} does not have any of the required roles: {}",
                    currentUsername, String.join(", ", roles));
            throw new AccessDeniedException("Access denied: None of the required roles found: " +
                    String.join(", ", roles));
        }
    }

    /**
     * Validate that the current user is an admin
     */
    public static void validateAdmin() {
        validateRole("ADMIN");
    }

    /**
     * Check if there is an authenticated user
     */
    public static boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null &&
                    authentication.isAuthenticated() &&
                    authentication.getPrincipal() instanceof UserPrincipal;
        } catch (Exception e) {
            log.debug("Error checking authentication status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get current user information safely (for logging purposes)
     */
    public static String getCurrentUserInfo() {
        try {
            UserPrincipal user = getCurrentUser();
            return String.format("User[id=%d, username=%s]", user.getUserId(), user.getUsername());
        } catch (Exception e) {
            return "User[anonymous]";
        }
    }

    /**
     * Execute an action only if the user is authenticated
     */
    public static void ifAuthenticated(Runnable action) {
        if (isAuthenticated()) {
            try {
                action.run();
            } catch (Exception e) {
                log.error("Error executing authenticated action: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Execute an action only if the user has the specified role
     */
    public static void ifHasRole(String role, Runnable action) {
        if (hasRole(role)) {
            try {
                action.run();
            } catch (Exception e) {
                log.error("Error executing role-based action for role '{}': {}", role, e.getMessage(), e);
            }
        }
    }

    /**
     * Execute an action only if the user is an admin
     */
    public static void ifAdmin(Runnable action) {
        ifHasRole("ADMIN", action);
    }
}