package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.request.StreakUpdateRequest;
import com.muscledia.Gamification_service.dto.response.ApiResponse;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.service.UserGamificationService;
import com.muscledia.Gamification_service.utils.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * User Gamification Controller - Simplified for MVP
 * Removed complex Swagger annotations to avoid dependency issues
 */
@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class UserGamificationController {

    private final UserGamificationService userGamificationService;

    /**
     * Create or get user gamification profile (for current user)
     */
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> createOrGetUserProfile() {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Creating or getting profile for current user {}", userId);

        try {
            UserGamificationProfile profile = userGamificationService.createOrGetUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", profile));
        } catch (Exception e) {
            log.error("Error creating/getting user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create or retrieve user profile"));
        }
    }

    /**
     * Get user gamification profile (for current user)
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> getUserProfile() {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting profile for current user {}", userId);

        try {
            UserGamificationProfile profile = userGamificationService.getUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success(profile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user profile"));
        }
    }

    /**
     * Update points for current user
     */
    @PutMapping("/points")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> updateUserPoints(
            @RequestParam @Min(value = 1, message = "Points to add must be positive") int pointsToAdd) {

        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Updating points for current user {} with {} points", userId, pointsToAdd);

        try {
            UserGamificationProfile updatedProfile = userGamificationService.updateUserPoints(userId, pointsToAdd);
            return ResponseEntity.ok(ApiResponse.success("Points updated successfully", updatedProfile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user points", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update user points"));
        }
    }

    /**
     * Update streak for current user
     */
    @PutMapping("/streaks")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> updateUserStreak(
            @Valid @RequestBody StreakUpdateRequest request) {

        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Updating streak for current user {} - type: {}, continues: {}",
                userId, request.getStreakType(), request.getStreakContinues());

        try {
            UserGamificationProfile updatedProfile = userGamificationService.updateUserStreak(
                    userId, request.getStreakType(), request.getStreakContinues());
            return ResponseEntity.ok(ApiResponse.success("Streak updated successfully", updatedProfile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating user streak", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update user streak"));
        }
    }

    /**
     * Get current user's current streak
     */
    @GetMapping("/streaks/{streakType}/current")
    public ResponseEntity<ApiResponse<Integer>> getCurrentUserCurrentStreak(@PathVariable String streakType) {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting current streak for current user {} and type {}", userId, streakType);

        try {
            int currentStreak = userGamificationService.getUserCurrentStreak(userId, streakType);
            return ResponseEntity.ok(ApiResponse.success(currentStreak));
        } catch (Exception e) {
            log.error("Error getting user current streak", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve current streak"));
        }
    }

    /**
     * Get current user's longest streak
     */
    @GetMapping("/streaks/{streakType}/longest")
    public ResponseEntity<ApiResponse<Integer>> getCurrentUserLongestStreak(@PathVariable String streakType) {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting longest streak for current user {} and type {}", userId, streakType);

        try {
            int longestStreak = userGamificationService.getUserLongestStreak(userId, streakType);
            return ResponseEntity.ok(ApiResponse.success(longestStreak));
        } catch (Exception e) {
            log.error("Error getting user longest streak", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve longest streak"));
        }
    }

    /**
     * Get current user's points rank
     */
    @GetMapping("/rank/points")
    public ResponseEntity<ApiResponse<Long>> getCurrentUserPointsRank() {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting points rank for current user {}", userId);

        try {
            long rank = userGamificationService.getUserPointsRank(userId);
            return ResponseEntity.ok(ApiResponse.success(rank));
        } catch (Exception e) {
            log.error("Error getting user points rank", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user points rank"));
        }
    }

    /**
     * Get current user's level rank
     */
    @GetMapping("/rank/level")
    public ResponseEntity<ApiResponse<Long>> getCurrentUserLevelRank() {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting level rank for current user {}", userId);

        try {
            long rank = userGamificationService.getUserLevelRank(userId);
            return ResponseEntity.ok(ApiResponse.success(rank));
        } catch (Exception e) {
            log.error("Error getting user level rank", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user level rank"));
        }
    }

    /**
     * Get current user's achievements summary
     */
    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUserAchievementsSummary() {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting achievements summary for current user {}", userId);

        try {
            Map<String, Object> summary = userGamificationService.getUserAchievementsSummary(userId);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting user achievements summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve achievements summary"));
        }
    }

    // ADMIN ENDPOINTS - Keep userId in path for admin operations
    /**
     * Reset user progress - Admin only
     */
    @PutMapping("/admin/{userId}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> resetUserProgress(@PathVariable Long userId) {
        log.info("Resetting progress for user {} - Requested by admin {}", userId,
                AuthenticationService.getCurrentUserId());

        try {
            UserGamificationProfile resetProfile = userGamificationService.resetUserProgress(userId);
            return ResponseEntity.ok(ApiResponse.success("User progress reset successfully", resetProfile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error resetting user progress", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reset user progress"));
        }
    }

    /**
     * Delete user profile - Admin only
     */
    @DeleteMapping("/admin/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUserProfile(@PathVariable Long userId) {
        log.info("Deleting profile for user {} - Requested by admin {}", userId,
                AuthenticationService.getCurrentUserId());

        try {
            userGamificationService.deleteUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success("User profile deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete user profile"));
        }
    }

    // PUBLIC LEADERBOARD ENDPOINTS - No user ID needed
    @GetMapping("/leaderboards/points")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getPointsLeaderboard(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        log.info("Getting points leaderboard with limit {} - Requested by user {}", limit,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getPointsLeaderboard(limit);
            return ResponseEntity.ok(ApiResponse.success("Points leaderboard retrieved successfully", users));
        } catch (Exception e) {
            log.error("Error getting points leaderboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve points leaderboard"));
        }
    }

    @GetMapping("/leaderboards/levels")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getLevelLeaderboard(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        log.info("Getting level leaderboard with limit {} - Requested by user {}", limit,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getLevelLeaderboard(limit);
            return ResponseEntity.ok(ApiResponse.success("Level leaderboard retrieved successfully", users));
        } catch (Exception e) {
            log.error("Error getting level leaderboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve level leaderboard"));
        }
    }

}