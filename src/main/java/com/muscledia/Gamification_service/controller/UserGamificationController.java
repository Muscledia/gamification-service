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

/**
 * User Gamification Controller - Simplified for MVP
 * Removed complex Swagger annotations to avoid dependency issues
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserGamificationController {

    private final UserGamificationService userGamificationService;

    /**
     * Create or get user gamification profile
     */
    @PostMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> createOrGetUserProfile(
            @PathVariable Long userId) {

        log.info("Creating or getting profile for user {} - Requested by user {}", userId,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Get user gamification profile
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> getUserProfile(
            @PathVariable Long userId) {

        log.info("Getting profile for user {} - Requested by user {}", userId,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Update user points
     */
    @PutMapping("/{userId}/points")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> updateUserPoints(
            @PathVariable Long userId,
            @RequestParam @Min(value = 1, message = "Points to add must be positive") int pointsToAdd) {

        log.info("Updating points for user {} with {} points - Requested by user {}", userId, pointsToAdd,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Update user streak
     */
    @PutMapping("/{userId}/streaks")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> updateUserStreak(
            @PathVariable Long userId,
            @Valid @RequestBody StreakUpdateRequest request) {

        log.info("Updating streak for user {} - type: {}, continues: {} - Requested by user {}",
                userId, request.getStreakType(), request.getStreakContinues(),
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Get user current streak
     */
    @GetMapping("/{userId}/streaks/{streakType}/current")
    public ResponseEntity<ApiResponse<Integer>> getUserCurrentStreak(
            @PathVariable Long userId,
            @PathVariable String streakType) {

        log.info("Getting current streak for user {} and type {} - Requested by user {}", userId, streakType,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Get user longest streak
     */
    @GetMapping("/{userId}/streaks/{streakType}/longest")
    public ResponseEntity<ApiResponse<Integer>> getUserLongestStreak(
            @PathVariable Long userId,
            @PathVariable String streakType) {

        log.info("Getting longest streak for user {} and type {} - Requested by user {}", userId, streakType,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Get user points rank
     */
    @GetMapping("/{userId}/rank/points")
    public ResponseEntity<ApiResponse<Long>> getUserPointsRank(
            @PathVariable Long userId) {

        log.info("Getting points rank for user {} - Requested by user {}", userId,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Get user level rank
     */
    @GetMapping("/{userId}/rank/level")
    public ResponseEntity<ApiResponse<Long>> getUserLevelRank(
            @PathVariable Long userId) {

        log.info("Getting level rank for user {} - Requested by user {}", userId,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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
     * Get user achievements summary
     */
    @GetMapping("/{userId}/achievements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserAchievementsSummary(
            @PathVariable Long userId) {

        log.info("Getting achievements summary for user {} - Requested by user {}", userId,
                AuthenticationService.getCurrentUserId());

        // Validate user access
        AuthenticationService.validateUserAccess(userId);

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

    /**
     * Reset user progress - Admin only
     */
    @PutMapping("/{userId}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> resetUserProgress(
            @PathVariable Long userId) {

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
    @DeleteMapping("/{userId}/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUserProfile(
            @PathVariable Long userId) {

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

    /**
     * Get points leaderboard - Public access for authenticated users
     */
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

    /**
     * Get level leaderboard - Public access for authenticated users
     */
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

    /**
     * Get streak leaderboard - Public access for authenticated users
     */
    @GetMapping("/leaderboards/streaks/{streakType}")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getStreakLeaderboard(
            @PathVariable String streakType,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.info("Getting streak leaderboard for type {} with limit {} - Requested by user {}", streakType, limit,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getStreakLeaderboard(streakType, limit);
            return ResponseEntity.ok(ApiResponse.success("Streak leaderboard retrieved successfully", users));
        } catch (Exception e) {
            log.error("Error getting streak leaderboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve streak leaderboard"));
        }
    }

    /**
     * Get longest streak leaderboard - Public access for authenticated users
     */
    @GetMapping("/leaderboards/streaks/{streakType}/longest")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getLongestStreakLeaderboard(
            @PathVariable String streakType,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.info("Getting longest streak leaderboard for type {} with limit {} - Requested by user {}", streakType,
                limit, AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getLongestStreakLeaderboard(streakType,
                    limit);
            return ResponseEntity.ok(ApiResponse.success("Longest streak leaderboard retrieved successfully", users));
        } catch (Exception e) {
            log.error("Error getting longest streak leaderboard", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve longest streak leaderboard"));
        }
    }

    /**
     * Get recent level ups - Accessible to all authenticated users
     */
    @GetMapping("/analytics/recent-levelups")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getRecentLevelUps(
            @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hoursBack) {

        log.info("Getting recent level ups within {} hours - Requested by user {}", hoursBack,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> recentLevelUps = userGamificationService.getRecentLevelUps(hoursBack);
            return ResponseEntity.ok(ApiResponse.success(recentLevelUps));
        } catch (Exception e) {
            log.error("Error getting recent level ups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve recent level ups"));
        }
    }

    /**
     * Get users with active streak - Accessible to all authenticated users
     */
    @GetMapping("/analytics/active-streaks/{streakType}")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getUsersWithActiveStreak(
            @PathVariable String streakType) {

        log.info("Getting users with active streak of type {} - Requested by user {}", streakType,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getUsersWithActiveStreak(streakType);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("Error getting users with active streak", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve users with active streak"));
        }
    }

    /**
     * Get users with minimum streak - Accessible to all authenticated users
     */
    @GetMapping("/analytics/minimum-streaks/{streakType}")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getUsersWithMinimumStreak(
            @PathVariable String streakType,
            @RequestParam @Min(1) int minLength) {

        log.info("Getting users with minimum streak of {} for type {} - Requested by user {}", minLength, streakType,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getUsersWithMinimumStreak(streakType,
                    minLength);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("Error getting users with minimum streak", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve users with minimum streak"));
        }
    }

    /**
     * Get platform statistics - Admin only
     */
    @GetMapping("/analytics/platform-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformStatistics() {

        log.info("Getting platform statistics - Requested by admin {}", AuthenticationService.getCurrentUserId());

        try {
            Map<String, Object> statistics = userGamificationService.getPlatformStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("Error getting platform statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve platform statistics"));
        }
    }
}