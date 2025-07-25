package com.muscledia.Gamification_service.event.handler;

import com.muscledia.Gamification_service.event.ExerciseCompletedEvent;
import com.muscledia.Gamification_service.service.BadgeService;
import com.muscledia.Gamification_service.service.UserGamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for exercise completion events.
 * Processes individual exercise data for exercise-specific quests and badges.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExerciseEventHandler {

    private final BadgeService badgeService;
    private final UserGamificationService userGamificationService;

    @Transactional
    public void handleExerciseCompleted(ExerciseCompletedEvent event) {
        log.debug("Processing exercise completion for user {} - {}",
                event.getUserId(), event.getExerciseName());

        try {
            // Award points for exercise completion
            awardExercisePoints(event);

            // Evaluate exercise-specific badges
            evaluateExerciseBadges(event);

            log.debug("Successfully processed exercise completion for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Error processing exercise completion for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    private void awardExercisePoints(ExerciseCompletedEvent event) {
        try {
            int basePoints = 10; // Base points per exercise
            int volumeBonus = event.isVolumeExercise() ? (int) (event.getVolume() / 1000) : 0; // 1 point per 1000
                                                                                               // volume
            int intensityBonus = calculateIntensityBonus(event);

            int totalPoints = basePoints + volumeBonus + intensityBonus;

            userGamificationService.updateUserPoints(event.getUserId(), totalPoints);

            log.debug("Awarded {} points to user {} for exercise {}",
                    totalPoints, event.getUserId(), event.getExerciseName());

        } catch (Exception e) {
            log.error("Error awarding exercise points to user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private void evaluateExerciseBadges(ExerciseCompletedEvent event) {
        try {
            Map<String, Object> exerciseStats = buildExerciseStats(event);

            var eligibleBadges = badgeService.getEligibleBadges(event.getUserId(), exerciseStats);

            for (var badge : eligibleBadges) {
                try {
                    badgeService.awardBadge(event.getUserId(), badge.getBadgeId());
                    log.debug("Awarded exercise badge {} to user {}",
                            badge.getName(), event.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to award exercise badge {} to user {}: {}",
                            badge.getBadgeId(), event.getUserId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error evaluating exercise badges for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private int calculateIntensityBonus(ExerciseCompletedEvent event) {
        String intensity = event.getIntensityLevel();

        return switch (intensity) {
            case "HIGH" -> 15;
            case "MEDIUM" -> 10;
            case "LOW" -> 5;
            default -> 0;
        };
    }

    private Map<String, Object> buildExerciseStats(ExerciseCompletedEvent event) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("exerciseName", event.getExerciseName());
        stats.put("exerciseCategory", event.getExerciseCategory());
        stats.put("setsCompleted", event.getSetsCompleted());
        stats.put("totalReps", event.getTotalReps());
        stats.put("volume", event.getVolume());
        stats.put("isVolumeExercise", event.isVolumeExercise());
        stats.put("isCardioExercise", event.isCardioExercise());
        stats.put("intensityLevel", event.getIntensityLevel());

        if (event.getWeight() != null) {
            stats.put("weight", event.getWeight());
            stats.put("weightUnit", event.getWeightUnit());
        }

        if (event.getDurationSeconds() != null) {
            stats.put("durationSeconds", event.getDurationSeconds());
        }

        if (event.getDistance() != null) {
            stats.put("distance", event.getDistance());
            stats.put("distanceUnit", event.getDistanceUnit());
        }

        return stats;
    }
}