package com.muscledia.Gamification_service.event.handler;

import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * REFACTORED: Focus on Signal - user-facing achievements only
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class WorkoutEventHandler {

    private final UserGamificationService userGamificationService;
    private final AchievementService achievementService;
    private final StreakService streakService;
    private final FitnessCoinsService fitnessCoinsService;
    private final ChallengeProgressService challengeProgressService;

    @Transactional
    public void handleWorkoutCompleted(WorkoutCompletedEvent event) {
        Long userId = event.getUserId();

        try {
            // 1. Update streaks
            StreakService.StreakUpdateResult streakResult = streakService.updateStreaks(userId, event.getTimestamp());

            // 2. Increment workout count & duration
            var profile = userGamificationService.createOrGetUserProfile(userId);
            profile.incrementWorkoutCount();

            // Track total workout time
            if (event.getDurationMinutes() != null) {
                int currentMinutes = profile.getTotalWorkoutMinutes() != null ?
                        profile.getTotalWorkoutMinutes() : 0;
                profile.setTotalWorkoutMinutes(currentMinutes + event.getDurationMinutes());
            }

            userGamificationService.saveUserProfile(profile);

            // 3. Award XP points (for leveling)
            int xpPoints = calculateXPPoints(event);
            var levelUpResult = userGamificationService.updateUserPoints(userId, xpPoints);

            // 4. Award Fitness Coins (spendable currency) - NOW BLOCKING
            fitnessCoinsService.awardWorkoutCoins(
                    userId,
                    event.getDurationMinutes() != null ? event.getDurationMinutes() : 0,
                    0,  // PRs handled separately
                    streakResult.getWeeklyStreak() != null ? streakResult.getWeeklyStreak() : 0
            );

            // 5. Update workout streak
            boolean streakEligible = event.getDurationMinutes() != null &&
                    event.getDurationMinutes() >= 15;
            userGamificationService.updateUserStreak(userId, "workout", streakEligible);

            // 6. Process achievements
            achievementService.processWorkoutAchievements(event);

            // SIGNAL: Log only meaningful events
            logUserFacingEvents(userId, xpPoints, levelUpResult != null, streakResult);

        } catch (Exception e) {
            log.error("Workout processing failed for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * SIMPLIFIED: Clear point formula
     */
    private int calculateXPPoints(WorkoutCompletedEvent event) {
        int base = 50;  // Base XP for completion

        // Simple duration bonus
        if (event.getDurationMinutes() != null) {
            if (event.getDurationMinutes() >= 60) {
                base += 30;
            } else if (event.getDurationMinutes() >= 45) {
                base += 25;
            } else if (event.getDurationMinutes() >= 30) {
                base += 15;
            }
        }

        return base;
    }

    /**
     * SIGNAL OVER NOISE: Only log celebrations
     */
    private void logUserFacingEvents(
            Long userId,
            int xpAwarded,
            boolean leveledUp,
            StreakService.StreakUpdateResult streakResult) {

        // Level up = exciting, always log
        if (leveledUp) {
            log.info("🎉 User {} LEVELED UP!", userId);
        }

        // Streak milestones = motivating
        if (streakResult != null && streakResult.getIsNewMilestone() != null && streakResult.getIsNewMilestone()) {
            log.info("🔥 User {} hit {}-day streak!", userId, streakResult.getCurrentStreak());
        }

        // Everything else = silent (tracked in DB)
    }


    /**
     * Update progress on active challenges
     */
    private void updateActiveChallenges(Long userId, WorkoutCompletedEvent event) {
        try {
            challengeProgressService.updateChallengeProgress(userId, event);
        } catch (Exception e) {
            log.warn("Failed to update challenges for user {}: {}", userId, e.getMessage());
            // Don't fail the entire workflow if challenge update fails
        }
    }
}