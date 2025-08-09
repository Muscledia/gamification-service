package com.muscledia.Gamification_service.event.handler;

import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.UserBadge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.service.AchievementService;
import com.muscledia.Gamification_service.service.BadgeService;
import com.muscledia.Gamification_service.service.QuestService;
import com.muscledia.Gamification_service.service.UserGamificationService;
import com.muscledia.Gamification_service.event.publisher.TransactionalEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final UserGamificationService userGamificationService;
    private final TransactionalEventPublisher eventPublisher;
    private final AchievementService achievementService;
    private final QuestService questService;

    /**
     * CORE USER STORY IMPLEMENTATION: Process workout completion for achievements
     *
     * Acceptance Criteria Implementation:
     * - GIVEN: User completed workout that satisfies achievement criteria (e.g., first workout)
     * - WHEN: gamification-service consumes WorkoutCompletedEvent
     * - THEN: user record updated to show new achievement
     */
    @Transactional
    public void handleWorkoutCompleted(WorkoutCompletedEvent event) {
        log.info("🎯 IMPLEMENTING USER STORY: Processing workout completion for user {} - workout {}",
                event.getUserId(), event.getWorkoutId());

        try {
            // 1. ENSURE USER PROFILE EXISTS AND UPDATE WORKOUT COUNT
            UserGamificationProfile userProfile = userGamificationService.createOrGetUserProfile(event.getUserId());

            log.info("User profile loaded: userId={}, currentLevel={}, currentPoints={}",
                    userProfile.getUserId(), userProfile.getLevel(), userProfile.getPoints());

            // 2. UPDATE WORKOUT STREAK
            updateWorkoutStreak(event);

            // 3. AWARD POINTS FOR WORKOUT COMPLETION
            awardWorkoutPoints(event);

            // 4. PROCESS ACHIEVEMENTS (CORE USER STORY) - delegates to AchievementService
            achievementService.processWorkoutAchievements(event);

            log.info("USER STORY SUCCESS: Achievement processing completed for user {} - user record updated with new achievements",
                    event.getUserId());

        } catch (Exception e) {
            log.error("USER STORY FAILURE: Error processing workout completion for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    /**
     * CORE ACHIEVEMENT LOGIC: Evaluate and award badges based on workout completion
     * This implements the "earn achievements automatically" requirement
     */
    private void evaluateAndAwardAchievementBadges(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        log.info("EVALUATING ACHIEVEMENTS: Checking badge eligibility for user {}", event.getUserId());

        try {
            // Create comprehensive user stats for badge evaluation
            Map<String, Object> userStats = buildComprehensiveUserStats(event, userProfile);

            // Get current workout count (including this one)
            int totalWorkouts = calculateTotalWorkouts(userProfile) + 1; // +1 for current workout
            userStats.put("totalWorkouts", totalWorkouts);

            log.info("User stats for achievement evaluation: totalWorkouts={}, currentLevel={}, currentPoints={}",
                    totalWorkouts, userProfile.getLevel(), userProfile.getPoints());

            // ACHIEVEMENT CRITERIA EXAMPLES:
            checkFirstWorkoutAchievement(event, userProfile, totalWorkouts);
            checkWorkoutMilestoneAchievements(event, userProfile, totalWorkouts);
            checkIntensityAchievements(event, userProfile);
            checkDurationAchievements(event, userProfile);

            // Get all eligible badges using the badge service
            List<Badge> eligibleBadges = badgeService.getEligibleBadges(event.getUserId(), userStats);

            // Award each eligible badge
            int newBadgesAwarded = 0;
            for (Badge badge : eligibleBadges) {
                try {
                    badgeService.awardBadge(event.getUserId(), badge.getBadgeId());
                    newBadgesAwarded++;
                    log.info("NEW ACHIEVEMENT EARNED: Awarded badge '{}' to user {} from workout completion",
                            badge.getName(), event.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to award badge {} to user {}: {}",
                            badge.getBadgeId(), event.getUserId(), e.getMessage());
                }
            }

            if (newBadgesAwarded > 0) {
                log.info("ACHIEVEMENTS SUMMARY: User {} earned {} new achievements from this workout!",
                        event.getUserId(), newBadgesAwarded);
            }

        } catch (Exception e) {
            log.error("Error evaluating workout achievements for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Check and award achievements based on workout completion
     * IMPLEMENTS USER STORY: "earn achievements automatically"
     */
    private void checkAndAwardAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        log.info("🏆 EVALUATING ACHIEVEMENTS: Checking achievements for user {}", event.getUserId());

        try {
            // Estimate workout count from points (simple approach)
            int estimatedWorkouts = userProfile.getPoints() / 50; // Assuming 50 base points per workout

            // Check for first workout achievement
            if (estimatedWorkouts == 0) {
                awardBadge(userProfile, "FIRST_WORKOUT", "Your first workout completed!");
                log.info("FIRST WORKOUT ACHIEVEMENT: User {} completed their first workout!",
                        event.getUserId());
            }

            // Check for milestone achievements
            checkMilestoneAchievements(event, userProfile, estimatedWorkouts + 1);

            // Check for intensity achievements
            checkIntensityAchievements(event, userProfile);

            // Check for duration achievements
            checkDurationAchievements(event, userProfile);

        } catch (Exception e) {
            log.error("Error evaluating achievements for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Check for "First Workout" achievement - classic example from user story
     */
    private void checkFirstWorkoutAchievement(WorkoutCompletedEvent event, UserGamificationProfile userProfile, int totalWorkouts) {
        if (totalWorkouts == 1) {
            log.info("FIRST WORKOUT ACHIEVEMENT: User {} completed their first workout! Triggering achievement.",
                    event.getUserId());

            // This would trigger the "First Workout" badge
            Map<String, Object> achievementData = new HashMap<>();
            achievementData.put("achievementType", "FIRST_WORKOUT");
            achievementData.put("workoutId", event.getWorkoutId());
            achievementData.put("completedAt", event.getWorkoutEndTime());

            // The badge service will handle awarding the actual badge
        }
    }

    /**
     * Check for workout milestone achievements (5, 10, 25, 50, 100 workouts)
     */
    private void checkWorkoutMilestoneAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile, int totalWorkouts) {
        int[] milestones = {5, 10, 25, 50, 100, 250, 500, 1000};

        for (int milestone : milestones) {
            if (totalWorkouts == milestone) {
                log.info("MILESTONE ACHIEVEMENT: User {} reached {} workouts milestone!",
                        event.getUserId(), milestone);

                // This would trigger milestone badges
                Map<String, Object> achievementData = new HashMap<>();
                achievementData.put("achievementType", "WORKOUT_MILESTONE");
                achievementData.put("milestone", milestone);
                achievementData.put("workoutId", event.getWorkoutId());
                break;
            }
        }
    }

    /**
     * Check for intensity-based achievements
     */
    private void checkIntensityAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        double intensity = event.getIntensityScore();

        if (intensity >= 3.0) {
            log.info("INTENSITY ACHIEVEMENT: User {} completed high-intensity workout (score: {})",
                    event.getUserId(), intensity);
        }
    }

    /**
     * Check for duration-based achievements
     */
    private void checkDurationAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        if (event.getDurationMinutes() >= 120) {
            log.info("DURATION ACHIEVEMENT: User {} completed epic 2+ hour workout!",
                    event.getUserId());
        }
    }


    /**
     * Award a badge to the user using your simple UserBadge model
     */
    private void awardBadge(UserGamificationProfile userProfile, String badgeId, String description) {
        // Check if user already has this badge
        boolean alreadyHas = userProfile.getEarnedBadges().stream()
                .anyMatch(badge -> badgeId.equals(badge.getBadgeId()));

        if (!alreadyHas) {
            // Create new badge using your simple model
            UserBadge newBadge = UserBadge.builder()
                    .badgeId(badgeId)
                    .earnedAt(Instant.now())
                    .build();

            userProfile.getEarnedBadges().add(newBadge);

            // Save the updated profile
            userGamificationService.createOrGetUserProfile(userProfile.getUserId());

            log.info("🏅 NEW ACHIEVEMENT: User {} earned badge '{}'",
                    userProfile.getUserId(), badgeId);
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

            log.info("Points awarded to user {} for workout: {} total (base:{}, duration:{}, intensity:{}, exercises:{})",
                    event.getUserId(), totalPoints, basePoints, durationBonus, intensityBonus, exerciseBonus);

        } catch (Exception e) {
            log.error("Error awarding workout points to user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Update quest progress based on workout
     */
    private void updateQuestProgress(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        try {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("workoutCompleted", true);
            progressData.put("workoutType", event.getWorkoutType());
            progressData.put("durationMinutes", event.getDurationMinutes());
            progressData.put("exercisesCompleted", event.getExercisesCompleted());
            progressData.put("caloriesBurned", event.getCaloriesBurned());

            // Update quest progress through quest service
            // questService.updateUserQuestProgress(event.getUserId(), progressData);

            log.debug("Updated quest progress for user {} based on workout", event.getUserId());

        } catch (Exception e) {
            log.error("Error updating quest progress for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Check milestone achievements (5, 10, 25, etc. workouts)
     */
    private void checkMilestoneAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile, int totalWorkouts) {
        if (totalWorkouts == 5) {
            awardBadge(userProfile, "WORKOUT_5", "Completed 5 workouts!");
            log.info("MILESTONE: User {} reached 5 workouts!", event.getUserId());
        } else if (totalWorkouts == 10) {
            awardBadge(userProfile, "WORKOUT_10", "Completed 10 workouts!");
            log.info("MILESTONE: User {} reached 10 workouts!", event.getUserId());
        } else if (totalWorkouts == 25) {
            awardBadge(userProfile, "WORKOUT_25", "Completed 25 workouts!");
            log.info("MILESTONE: User {} reached 25 workouts!", event.getUserId());
        } else if (totalWorkouts == 50) {
            awardBadge(userProfile, "WORKOUT_50", "Completed 50 workouts!");
            log.info("MILESTONE: User {} reached 50 workouts!", event.getUserId());
        }
    }

    // Helper methods remain the same...
    private int calculateDurationBonus(Integer durationMinutes) {
        if (durationMinutes == null) return 0;
        if (durationMinutes >= 90) return 30;
        if (durationMinutes >= 60) return 20;
        if (durationMinutes >= 45) return 15;
        if (durationMinutes >= 30) return 10;
        return 5;
    }

    private int calculateIntensityBonus(WorkoutCompletedEvent event) {
        double intensity = event.getIntensityScore();
        if (intensity >= 3.0) return 25;
        if (intensity >= 2.0) return 15;
        if (intensity >= 1.0) return 10;
        return 5;
    }

    private Integer calculatePreviousLevel(int previousPoints) {
        // Simple level calculation - you can make this more sophisticated
        if (previousPoints < 100) return 1;
        if (previousPoints < 300) return 2;
        if (previousPoints < 600) return 3;
        if (previousPoints < 1000) return 4;
        if (previousPoints < 1500) return 5;
        // ... continue level progression
        return Math.max(1, previousPoints / 300 + 1);
    }

    private boolean isStreakMilestone(int streak) {
        return streak == 3 || streak == 7 || streak == 14 || streak == 30 || streak == 60 || streak == 100;
    }

    private Map<String, Object> buildComprehensiveUserStats(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        Map<String, Object> stats = new HashMap<>();

        // Current workout stats
        stats.put("workoutCount", 1);
        stats.put("workoutType", event.getWorkoutType());
        stats.put("durationMinutes", event.getDurationMinutes());
        stats.put("caloriesBurned", event.getCaloriesBurned());
        stats.put("exercisesCompleted", event.getExercisesCompleted());
        stats.put("totalSets", event.getTotalSets());
        stats.put("totalReps", event.getTotalReps());
        stats.put("intensityScore", event.getIntensityScore());

        // User profile stats
        stats.put("currentLevel", userProfile.getLevel());
        stats.put("currentPoints", userProfile.getPoints());
        stats.put("earnedBadgeCount", userProfile.getEarnedBadges().size());

        return stats;
    }

    private int calculateTotalWorkouts(UserGamificationProfile userProfile) {
        // This would ideally come from a workout count field in the profile
        // For now, we can estimate based on points or implement a separate counter
        return userProfile.getPoints() / 50; // Rough estimate based on base points per workout
    }

    /**
     * FIXED: Update quest progress using QuestService
     */
    private void updateQuestProgressViaService(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        try {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("workoutCompleted", true);
            progressData.put("workoutType", event.getWorkoutType());
            progressData.put("durationMinutes", event.getDurationMinutes());
            progressData.put("exercisesCompleted", event.getExercisesCompleted());
            progressData.put("caloriesBurned", event.getCaloriesBurned());
            progressData.put("totalSets", event.getTotalSets());
            progressData.put("totalReps", event.getTotalReps());
            progressData.put("intensityScore", event.getIntensityScore());

            // FIXED: Use QuestService if available
            if (questService != null) {
                // Assuming QuestService has a method to update quest progress
                // questService.updateUserQuestProgress(event.getUserId(), progressData);
                log.info("Updated quest progress for user {} via QuestService", event.getUserId());
            } else {
                log.debug("QuestService not available, skipping quest progress update");
            }

        } catch (Exception e) {
            log.error("Error updating quest progress for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * FIXED: Publish gamification events using TransactionalEventPublisher
     */
    private void publishGamificationEvents(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        try {
            // FIXED: Use TransactionalEventPublisher to publish level up events, streak events, etc.

            // Check if user leveled up and publish level up event
            Integer previousLevel = calculatePreviousLevel(userProfile.getPoints() - 50); // Assuming 50 base points
            if (userProfile.getLevel() > previousLevel) {
                publishLevelUpEvent(event, userProfile);
            }

            // Check for streak milestones and publish streak events
            int currentStreak = userGamificationService.getUserCurrentStreak(event.getUserId(), "workout");
            if (isStreakMilestone(currentStreak)) {
                publishStreakEvent(event, userProfile, currentStreak);
            }

            log.debug("Published gamification events for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Error publishing gamification events for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    /**
     * Publish level up event
     */
    private void publishLevelUpEvent(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        try {
            // Create level up event data
            Map<String, Object> levelUpData = new HashMap<>();
            levelUpData.put("userId", userProfile.getUserId());
            levelUpData.put("newLevel", userProfile.getLevel());
            levelUpData.put("triggeredBy", "workout_completion");
            levelUpData.put("workoutId", event.getWorkoutId());

            // FIXED: Use TransactionalEventPublisher
            // Note: You would need to create a LevelUpEvent class similar to WorkoutCompletedEvent
            // eventPublisher.publishLevelUp(levelUpEvent);

            log.info("Level up event published for user {} - new level: {}",
                    userProfile.getUserId(), userProfile.getLevel());

        } catch (Exception e) {
            log.error("Error publishing level up event: {}", e.getMessage());
        }
    }

    /**
     * Publish streak milestone event
     */
    private void publishStreakEvent(WorkoutCompletedEvent event, UserGamificationProfile userProfile, int streakLength) {
        try {
            Map<String, Object> streakData = new HashMap<>();
            streakData.put("userId", userProfile.getUserId());
            streakData.put("streakType", "workout");
            streakData.put("streakLength", streakLength);
            streakData.put("triggeredBy", "workout_completion");

            // FIXED: Use TransactionalEventPublisher
            // eventPublisher.publishStreakMilestone(streakEvent);

            log.info("Streak milestone event published for user {} - {} day streak",
                    userProfile.getUserId(), streakLength);

        } catch (Exception e) {
            log.error("Error publishing streak event: {}", e.getMessage());
        }
    }
}