package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.request.StreakUpdateRequest;
import com.muscledia.Gamification_service.dto.response.ApiResponse;
import com.muscledia.Gamification_service.dto.response.LeaderboardResponse;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.service.UserGamificationService;
import com.muscledia.Gamification_service.utils.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "User Gamification Management", description = "Operations for managing user gamification profiles, progress tracking, leaderboards, and analytics")
@SecurityRequirement(name = "bearerAuth")
public class UserGamificationController {

    private final UserGamificationService userGamificationService;

    /**
     * Create or get user gamification profile
     */
    @PostMapping("/{userId}/profile")
    @Operation(summary = "Create or get user gamification profile", description = "Creates a new gamification profile or retrieves existing profile for the specified user. Users can only access their own profiles unless they have admin role.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved/created successfully", content = @Content(schema = @Schema(implementation = UserGamificationProfile.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - can only access own profile", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<UserGamificationProfile>> createOrGetUserProfile(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

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
    @Operation(summary = "Get user gamification profile", description = "Retrieves the gamification profile for the specified user. Users can only access their own profiles unless they have admin role.")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> getUserProfile(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

        log.info("Getting profile for user {} - Requested by user {}", userId, AuthenticationService.getCurrentUserId());

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
    @Operation(summary = "Update user points", description = "Adds points to the specified user's gamification profile. Users can only update their own points unless they have admin role.")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> updateUserPoints(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Points to add (must be positive)", required = true) @RequestParam @Min(value = 1, message = "Points to add must be positive") int pointsToAdd) {

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
    @Operation(summary = "Update user streak", description = "Updates the streak information for the specified user. Users can only update their own streaks unless they have admin role.")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> updateUserStreak(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Streak update request", required = true) @Valid @RequestBody StreakUpdateRequest request) {

        log.info("Updating streak for user {} - type: {}, continues: {} - Requested by user {}",
                userId, request.getStreakType(), request.getStreakContinues(), AuthenticationService.getCurrentUserId());

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
    @Operation(summary = "Get user current streak", description = "Retrieves the current streak count for the specified user and streak type. Users can only access their own streaks unless they have admin role.")
    public ResponseEntity<ApiResponse<Integer>> getUserCurrentStreak(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Streak type", required = true) @PathVariable String streakType) {

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
    @Operation(summary = "Get user longest streak", description = "Retrieves the longest streak count for the specified user and streak type. Users can only access their own streaks unless they have admin role.")
    public ResponseEntity<ApiResponse<Integer>> getUserLongestStreak(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Streak type", required = true) @PathVariable String streakType) {

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
    @Operation(summary = "Get user points rank", description = "Retrieves the user's rank based on their total points. Users can only access their own rank unless they have admin role.")
    public ResponseEntity<ApiResponse<Long>> getUserPointsRank(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

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
    @Operation(summary = "Get user level rank", description = "Retrieves the user's rank based on their current level. Users can only access their own rank unless they have admin role.")
    public ResponseEntity<ApiResponse<Long>> getUserLevelRank(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

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
    @Operation(summary = "Get user achievements summary", description = "Retrieves a comprehensive summary of the user's achievements including badges, quests, and champions. Users can only access their own achievements unless they have admin role.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserAchievementsSummary(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

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
    @Operation(summary = "Reset user progress", description = "Resets all gamification progress for the specified user. This is an admin-only operation.")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> resetUserProgress(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

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
    @Operation(summary = "Delete user profile", description = "Permanently deletes the user's gamification profile and all associated data. This is an admin-only operation.")
    public ResponseEntity<ApiResponse<Void>> deleteUserProfile(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

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
    @Operation(summary = "Get points leaderboard", description = "Retrieves the top users ranked by total points. Available to all authenticated users.")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getPointsLeaderboard(
            @Parameter(description = "Maximum number of users to return (1-100)", required = false) @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.info("Getting points leaderboard with limit {} - Requested by user {}", limit,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getPointsLeaderboard(limit);
            LeaderboardResponse response = new LeaderboardResponse("points", null, limit, users.size(), users);
            return ResponseEntity.ok(ApiResponse.success(response));
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
    @Operation(summary = "Get level leaderboard", description = "Retrieves the top users ranked by their current level. Available to all authenticated users.")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLevelLeaderboard(
            @Parameter(description = "Maximum number of users to return (1-100)", required = false) @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.info("Getting level leaderboard with limit {} - Requested by user {}", limit,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getLevelLeaderboard(limit);
            LeaderboardResponse response = new LeaderboardResponse("levels", null, limit, users.size(), users);
            return ResponseEntity.ok(ApiResponse.success(response));
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
    @Operation(summary = "Get streak leaderboard", description = "Retrieves the top users ranked by their current streak for the specified type. Available to all authenticated users.")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getStreakLeaderboard(
            @Parameter(description = "Streak type", required = true) @PathVariable String streakType,
            @Parameter(description = "Maximum number of users to return (1-100)", required = false) @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.info("Getting streak leaderboard for type {} with limit {} - Requested by user {}", streakType, limit,
                AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getStreakLeaderboard(streakType, limit);
            LeaderboardResponse response = new LeaderboardResponse("current_streaks", streakType, limit, users.size(),
                    users);
            return ResponseEntity.ok(ApiResponse.success(response));
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
    @Operation(summary = "Get longest streak leaderboard", description = "Retrieves the top users ranked by their longest streak for the specified type. Available to all authenticated users.")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLongestStreakLeaderboard(
            @Parameter(description = "Streak type", required = true) @PathVariable String streakType,
            @Parameter(description = "Maximum number of users to return (1-100)", required = false) @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {

        log.info("Getting longest streak leaderboard for type {} with limit {} - Requested by user {}", streakType,
                limit, AuthenticationService.getCurrentUserId());

        try {
            List<UserGamificationProfile> users = userGamificationService.getLongestStreakLeaderboard(streakType,
                    limit);
            LeaderboardResponse response = new LeaderboardResponse("longest_streaks", streakType, limit, users.size(),
                    users);
            return ResponseEntity.ok(ApiResponse.success(response));
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
    @Operation(summary = "Get recent level ups", description = "Retrieves users who have recently leveled up within the specified time frame. Available to all authenticated users.")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getRecentLevelUps(
            @Parameter(description = "Hours to look back (1-168)", required = false) @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hoursBack) {

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
    @Operation(summary = "Get users with active streak", description = "Retrieves users who currently have an active streak of the specified type. Available to all authenticated users.")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getUsersWithActiveStreak(
            @Parameter(description = "Streak type", required = true) @PathVariable String streakType) {

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
    @Operation(summary = "Get users with minimum streak", description = "Retrieves users who have achieved at least the specified minimum streak length. Available to all authenticated users.")
    public ResponseEntity<ApiResponse<List<UserGamificationProfile>>> getUsersWithMinimumStreak(
            @Parameter(description = "Streak type", required = true) @PathVariable String streakType,
            @Parameter(description = "Minimum streak length", required = true) @RequestParam @Min(1) int minLength) {

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
    @Operation(summary = "Get platform statistics", description = "Retrieves comprehensive platform statistics including user counts, average levels, and engagement metrics. This is an admin-only operation.")
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