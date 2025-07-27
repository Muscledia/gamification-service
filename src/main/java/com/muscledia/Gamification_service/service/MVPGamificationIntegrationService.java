package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.*;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SIMPLIFIED MVP Integration Service
 * 
 * Keep it simple for MVP:
 * ✅ Process workouts → award points → update streaks
 * ✅ Fast in-memory caching
 * ✅ Zero Redis costs
 * ✅ Real-time user feedback
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class MVPGamificationIntegrationService {

    private final UserGamificationService userGamificationService;

    /**
     * SIMPLE WORKOUT FLOW - MVP Focus
     * Just the essentials: Points + Streaks + Fast Response
     */
    public Map<String, Object> processWorkoutCompletion(WorkoutCompletedEvent workoutEvent) {
        log.info("🏋️ Processing workout for user {} - Simple MVP", workoutEvent.getUserId());

        Instant startTime = Instant.now();
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Award Points (Most Important!)
            int pointsAwarded = calculateSimpleWorkoutPoints(workoutEvent);
            UserGamificationProfile userProfile = userGamificationService.updateUserPoints(
                    workoutEvent.getUserId(), pointsAwarded);

            // 2. Update Streak (Simple)
            userGamificationService.updateUserStreak(
                    workoutEvent.getUserId(),
                    "workout",
                    workoutEvent.isStreakEligible());

            // 3. Build Simple Response
            result.put("success", true);
            result.put("pointsAwarded", pointsAwarded);
            result.put("newLevel", userProfile.getLevel());
            result.put("totalPoints", userProfile.getPoints());

            long processingTimeMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
            result.put("processingTimeMs", processingTimeMs);

            log.info(" Workout processed in {}ms - {} points awarded",
                    processingTimeMs, pointsAwarded);

            return result;

        } catch (Exception e) {
            log.error(" Error processing workout for user {}: {}",
                    workoutEvent.getUserId(), e.getMessage());

            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Simple leaderboard (no complex caching)
     */
    public Map<String, Object> getSimpleLeaderboard(int limit) {
        Instant startTime = Instant.now();

        // Just get top users by points - simple!
        List<UserGamificationProfile> topUsers = userGamificationService.getTopUsersByPoints(limit);

        long responseTimeMs = java.time.Duration.between(startTime, Instant.now()).toMillis();

        return Map.of(
                "leaderboard", topUsers,
                "responseTimeMs", responseTimeMs,
                "simple", true,
                "cost", "$0");
    }

    /**
     * MVP Health Status - Keep it simple
     */
    public Map<String, Object> getSimpleMVPStatus() {
        return Map.of(
                "status", "Simple MVP Ready",
                "features", List.of("Points", "Levels", "Streaks", "Leaderboards"),
                "caching", "In-Memory Only",
                "cost", "$0 additional infrastructure",
                "performance", "Fast enough for MVP",
                "complexity", "Minimal - Easy to maintain");
    }

    // ===============================
    // SIMPLE HELPER METHODS
    // ===============================

    /**
     * Simple point calculation - no over-engineering
     */
    private int calculateSimpleWorkoutPoints(WorkoutCompletedEvent event) {
        int basePoints = 50;
        int durationBonus = Math.min(event.getDurationMinutes() / 10, 20); // Max 20 bonus
        int exerciseBonus = event.getExercisesCompleted() * 3; // 3 points per exercise

        return basePoints + durationBonus + exerciseBonus;
    }
}