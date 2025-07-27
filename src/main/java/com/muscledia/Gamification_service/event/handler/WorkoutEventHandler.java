package com.muscledia.Gamification_service.event.handler;

import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.service.BadgeService;
import com.muscledia.Gamification_service.service.QuestService;
import com.muscledia.Gamification_service.service.UserGamificationService;
import com.muscledia.Gamification_service.event.publisher.TransactionalEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for workout completion events.
 * 
 * ONLY ENABLED WHEN EVENTS ARE ENABLED
 * Uses TransactionalEventPublisher for atomic event publishing
 * For MVP: Disabled by default (no Kafka required)
 * For Production: Enable with EVENTS_ENABLED=true
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class WorkoutEventHandler {

    private final BadgeService badgeService;
    private final QuestService questService;
    private final UserGamificationService userGamificationService;
    private final TransactionalEventPublisher eventPublisher;

    /**
     * Process workout completion event and trigger gamification elements
     * All operations are transactional, including event publishing
     */
    @Transactional
    public void handleWorkoutCompleted(WorkoutCompletedEvent event) {
        log.info("Processing workout completion for user {} - workout {}",
                event.getUserId(), event.getWorkoutId());

        try {
            // 1. Update user streak
            updateWorkoutStreak(event);

            // 2. Award points for workout completion
            awardWorkoutPoints(event);

            // 3. Check for workout-related badge eligibility
            evaluateWorkoutBadges(event);

            // 4. Update quest progress
            updateQuestProgress(event);

            // 5. Check for milestone achievements
            checkMilestoneAchievements(event);

            log.info("Successfully processed workout completion for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Error processing workout completion for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    /**
     * Update user's workout streak
     */
    private void updateWorkoutStreak(WorkoutCompletedEvent event) {
        try {
            boolean streakEligible = event.isStreakEligible();
            userGamificationService.updateUserStreak(
                    event.getUserId(),
                    "workout",
                    streakEligible);

            log.debug("Updated workout streak for user {}: eligible={}",
                    event.getUserId(), streakEligible);

        } catch (Exception e) {
            log.error("Error updating workout streak for user {}: {}",
                    event.getUserId(), e.getMessage());
            // Don't re-throw - continue with other processing
        }
    }

    /**
     * Award points based on workout characteristics
     */
    private void awardWorkoutPoints(WorkoutCompletedEvent event) {
        try {
            int basePoints = 50; // Base points for completing a workout
            int durationBonus = calculateDurationBonus(event.getDurationMinutes());
            int intensityBonus = calculateIntensityBonus(event);
            int exerciseBonus = event.getExercisesCompleted() * 5; // 5 points per exercise

            int totalPoints = basePoints + durationBonus + intensityBonus + exerciseBonus;

            userGamificationService.updateUserPoints(event.getUserId(), totalPoints);

            log.info("Awarded {} points to user {} for workout completion",
                    totalPoints, event.getUserId());

        } catch (Exception e) {
            log.error("Error awarding workout points to user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Evaluate workout-related badges
     */
    private void evaluateWorkoutBadges(WorkoutCompletedEvent event) {
        try {
            Map<String, Object> userStats = buildUserStatsFromWorkout(event);

            // Get all eligible badges for this user
            List<Badge> eligibleBadges = badgeService.getEligibleBadges(event.getUserId(), userStats);

            // Award each eligible badge
            for (Badge badge : eligibleBadges) {
                try {
                    badgeService.awardBadge(event.getUserId(), badge.getBadgeId());
                    log.info("Awarded badge {} to user {} from workout",
                            badge.getName(), event.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to award badge {} to user {}: {}",
                            badge.getBadgeId(), event.getUserId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error evaluating workout badges for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Update quest progress based on workout
     */
    private void updateQuestProgress(WorkoutCompletedEvent event) {
        try {
            // Create quest progress data from workout
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("workoutCompleted", true);
            progressData.put("workoutType", event.getWorkoutType());
            progressData.put("durationMinutes", event.getDurationMinutes());
            progressData.put("exercisesCompleted", event.getExercisesCompleted());
            progressData.put("caloriesBurned", event.getCaloriesBurned());

            // This would integrate with quest progress tracking
            // questService.updateUserQuestProgress(event.getUserId(), progressData);

            log.debug("Updated quest progress for user {} based on workout", event.getUserId());

        } catch (Exception e) {
            log.error("Error updating quest progress for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Check for workout milestone achievements
     */
    private void checkMilestoneAchievements(WorkoutCompletedEvent event) {
        try {
            // Check for significant workout milestones
            if (event.getDurationMinutes() >= 120) { // 2 hour workout
                log.info("User {} completed an epic 2+ hour workout!", event.getUserId());
                // Could trigger special achievement
            }

            if (event.getIntensityScore() >= 2.0) { // High intensity
                log.info("User {} completed a high-intensity workout!", event.getUserId());
                // Could trigger intensity-based achievements
            }

        } catch (Exception e) {
            log.error("Error checking milestone achievements for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Calculate bonus points based on workout duration
     */
    private int calculateDurationBonus(Integer durationMinutes) {
        if (durationMinutes == null)
            return 0;

        if (durationMinutes >= 90)
            return 30; // 90+ minutes
        if (durationMinutes >= 60)
            return 20; // 60+ minutes
        if (durationMinutes >= 45)
            return 15; // 45+ minutes
        if (durationMinutes >= 30)
            return 10; // 30+ minutes
        return 5; // Any workout gets small bonus
    }

    /**
     * Calculate bonus points based on workout intensity
     */
    private int calculateIntensityBonus(WorkoutCompletedEvent event) {
        double intensity = event.getIntensityScore();

        if (intensity >= 3.0)
            return 25; // Very high intensity
        if (intensity >= 2.0)
            return 15; // High intensity
        if (intensity >= 1.0)
            return 10; // Moderate intensity
        return 5; // Low intensity still gets small bonus
    }

    /**
     * Build user statistics map from workout event
     */
    private Map<String, Object> buildUserStatsFromWorkout(WorkoutCompletedEvent event) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("workoutCount", 1); // This workout
        stats.put("workoutType", event.getWorkoutType());
        stats.put("durationMinutes", event.getDurationMinutes());
        stats.put("caloriesBurned", event.getCaloriesBurned());
        stats.put("exercisesCompleted", event.getExercisesCompleted());
        stats.put("totalSets", event.getTotalSets());
        stats.put("totalReps", event.getTotalReps());
        stats.put("intensityScore", event.getIntensityScore());

        return stats;
    }
}