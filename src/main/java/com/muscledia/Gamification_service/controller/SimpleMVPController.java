package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.service.MVPGamificationIntegrationService;
import com.muscledia.Gamification_service.service.UserGamificationService;
import com.muscledia.Gamification_service.service.cache.SimpleMVPCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import java.util.Map;

/**
 * SIMPLE MVP CONTROLLER WITH SWAGGER DOCUMENTATION
 * 
 * Essential gamification features with proper API documentation:
 * - Award points for workouts
 * - Show leaderboards
 * - Get user stats
 * - System monitoring
 */
@RestController
@RequestMapping("/api/mvp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MVP Gamification", description = "Core MVP gamification endpoints - simple and fast")
public class SimpleMVPController {

    private final UserGamificationService userGamificationService;
    private final SimpleMVPCacheService cacheService;
    private final MVPGamificationIntegrationService integrationService;

    /**
     * Award points for completing a workout
     */
    @PostMapping("/workout-completed")
    @Operation(summary = "Process workout completion", description = "Awards points and updates streaks when a user completes a workout")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workout processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid workout data")
    })
    public ResponseEntity<com.muscledia.Gamification_service.dto.response.ApiResponse<Map<String, Object>>> workoutCompleted(
            @Parameter(description = "Workout completion event data", required = true) @RequestBody WorkoutCompletedEvent workoutEvent) {

        log.info("MVP: Processing workout for user {}", workoutEvent.getUserId());

        Map<String, Object> result = integrationService.processWorkoutCompletion(workoutEvent);

        return ResponseEntity.ok(com.muscledia.Gamification_service.dto.response.ApiResponse
                .success("Workout processed successfully!", result));
    }

    /**
     * Get user's gamification profile
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user profile", description = "Retrieves a user's complete gamification profile including points, level, and streaks")
    public ResponseEntity<com.muscledia.Gamification_service.dto.response.ApiResponse<UserGamificationProfile>> getUserProfile(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId) {

        UserGamificationProfile profile = userGamificationService.getUserProfile(userId);

        return ResponseEntity.ok(
                com.muscledia.Gamification_service.dto.response.ApiResponse.success("User profile retrieved", profile));
    }

    /**
     * Award points manually (for testing)
     */
    @PostMapping("/users/{userId}/points")
    @Operation(summary = "Award points manually", description = "Manually award points to a user (useful for testing and admin operations)")
    public ResponseEntity<com.muscledia.Gamification_service.dto.response.ApiResponse<UserGamificationProfile>> awardPoints(
            @Parameter(description = "User ID", required = true) @PathVariable Long userId,
            @Parameter(description = "Number of points to award", required = true) @RequestParam int points) {

        UserGamificationProfile profile = userGamificationService.updateUserPoints(userId, points);

        return ResponseEntity.ok(com.muscledia.Gamification_service.dto.response.ApiResponse.success(
                String.format("Awarded %d points to user", points), profile));
    }

    /**
     * Get simple leaderboard (cached)
     */
    @GetMapping("/leaderboard")
    @Operation(summary = "Get leaderboard", description = "Retrieves the top users leaderboard (cached for performance)")
    public ResponseEntity<com.muscledia.Gamification_service.dto.response.ApiResponse<List<UserGamificationProfile>>> getLeaderboard(
            @Parameter(description = "Number of top users to return", required = false) @RequestParam(defaultValue = "10") int limit) {

        List<UserGamificationProfile> leaderboard = cacheService.getPointsLeaderboard(limit);

        return ResponseEntity.ok(com.muscledia.Gamification_service.dto.response.ApiResponse.success(
                String.format("Top %d users by points", limit), leaderboard));
    }

    /**
     * MVP Status and metrics
     */
    @GetMapping("/status")
    @Operation(summary = "Get MVP status", description = "Returns system status, performance metrics, and configuration info")
    public ResponseEntity<com.muscledia.Gamification_service.dto.response.ApiResponse<Map<String, Object>>> getMVPStatus() {

        Map<String, Object> status = integrationService.getSimpleMVPStatus();

        // Add cache stats
        status.put("cacheStats", cacheService.getCacheStats());

        return ResponseEntity
                .ok(com.muscledia.Gamification_service.dto.response.ApiResponse.success("Simple MVP status", status));
    }

    /**
     * Clear cache (for testing)
     */
    @PostMapping("/cache/clear")
    @Operation(summary = "Clear cache", description = "Clears all cached data (useful for testing and debugging)")
    public ResponseEntity<com.muscledia.Gamification_service.dto.response.ApiResponse<String>> clearCache() {

        cacheService.clearCache();

        return ResponseEntity.ok(com.muscledia.Gamification_service.dto.response.ApiResponse
                .success("Cache cleared successfully", "Cache cleared"));
    }
}