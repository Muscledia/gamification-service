package com.muscledia.Gamification_service.event.handler;

import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.service.AchievementService;
import com.muscledia.Gamification_service.service.StreakService;
import com.muscledia.Gamification_service.service.UserGamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


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

    private final UserGamificationService userGamificationService;
    private final AchievementService achievementService;
    private final StreakService streakService;

    /**
     * CORE USER STORY IMPLEMENTATION: Process workout completion for achievements and challenges
     */
    @Transactional
    public void handleWorkoutCompleted(WorkoutCompletedEvent event) {
        log.info("Processing workout completion for user {} - workout {}",
                event.getUserId(), event.getWorkoutId());

        try {
            // 1. Update both weekly and monthly streaks
            streakService.updateStreaks(event.getUserId(), event.getTimestamp());

            // 2. Update user profile and workout count
            updateUserProfile(event);

            // 3. Award points for completing workout
            awardWorkoutPoints(event);

            // 4. Update workout streak (existing daily streak logic if you want to keep it)
            updateWorkoutStreak(event);

            // 5. Process achievements
            achievementService.processWorkoutAchievements(event);

            log.info("SUCCESS: Workout completion processed for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("FAILED: Error processing workout completion for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update user profile with workout completion
     */
    private void updateUserProfile(WorkoutCompletedEvent event) {
        var profile = userGamificationService.createOrGetUserProfile(event.getUserId());

        // Increment workout count
        int currentCount = profile.getTotalWorkoutsCompleted() != null ?
                profile.getTotalWorkoutsCompleted() : 0;
        profile.setTotalWorkoutsCompleted(currentCount + 1);

        userGamificationService.saveUserProfile(profile);

        log.debug("Updated workout count for user {} to {}", event.getUserId(), currentCount + 1);
    }

    /**
     * Award points for workout completion
     */
    private void awardWorkoutPoints(WorkoutCompletedEvent event) {
        int basePoints = 50;
        int bonusPoints = calculateBonusPoints(event);
        int totalPoints = basePoints + bonusPoints;

        userGamificationService.updateUserPoints(event.getUserId(), totalPoints);

        log.info("Awarded {} points to user {} (base: {}, bonus: {})",
                totalPoints, event.getUserId(), basePoints, bonusPoints);
    }

    /**
     * Calculate bonus points based on workout metrics
     */
    private int calculateBonusPoints(WorkoutCompletedEvent event) {
        int bonus = 0;

        // Duration bonus
        if (event.getDurationMinutes() != null) {
            if (event.getDurationMinutes() >= 60) bonus += 20;
            else if (event.getDurationMinutes() >= 30) bonus += 10;
            else bonus += 5;
        }

        // Exercise variety bonus
        if (event.getExercisesCompleted() != null) {
            bonus += Math.min(event.getExercisesCompleted() * 3, 30);
        }

        // Volume bonus
        if (event.getTotalSets() != null) {
            bonus += Math.min(event.getTotalSets() * 2, 40);
        }

        return bonus;
    }

    /**
     * Update workout streak
     */
    private void updateWorkoutStreak(WorkoutCompletedEvent event) {
        try {
            boolean streakEligible = event.getDurationMinutes() != null &&
                    event.getDurationMinutes() >= 15;

            userGamificationService.updateUserStreak(event.getUserId(), "workout", streakEligible);

            log.debug("Updated workout streak for user {}: eligible={}",
                    event.getUserId(), streakEligible);
        } catch (Exception e) {
            log.error("Failed to update streak for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}