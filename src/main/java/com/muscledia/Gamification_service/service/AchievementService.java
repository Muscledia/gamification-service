package com.muscledia.Gamification_service.service;


import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.model.UserBadge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * CLEAN Achievement Service - Direct badge awarding without complex dependencies
 * Implements: "Earn achievements automatically when completing specific actions"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final UserGamificationService userGamificationService;

    /**
     * Process workout completion and award achievements
     * IMPLEMENTS USER STORY: Check if workout satisfies criteria and update user record
     */
    @Transactional
    public void processWorkoutAchievements(WorkoutCompletedEvent event) {
        log.info("Processing achievements for user {} from workout {}",
                event.getUserId(), event.getWorkoutId());

        try {
            UserGamificationProfile userProfile = userGamificationService.createOrGetUserProfile(event.getUserId());

            // Calculate current workout count
            int workoutCount = userProfile.getTotalWorkoutsCompleted() != null ?
                    userProfile.getTotalWorkoutsCompleted() : 1;

            // Check all achievement types
            checkFirstWorkoutAchievement(userProfile, workoutCount);
            checkWorkoutMilestones(userProfile, workoutCount);
            checkStreakAchievements(userProfile, event);
            checkDurationAchievements(userProfile, event);
            checkVolumeAchievements(userProfile, event);

            // Save updated profile
            userGamificationService.saveUserProfile(userProfile);

            log.info("Achievement processing completed for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process achievements for user {}: {}", event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check and award "First Workout" achievement
     */
    private void checkFirstWorkoutAchievement(UserGamificationProfile userProfile, int workoutCount) {
        if (workoutCount == 1) {
            log.info("FIRST WORKOUT ACHIEVEMENT: User {} completed their first workout!", userProfile.getUserId());

            awardBadge(userProfile, "FIRST_WORKOUT", "First Steps",
                    "Congratulations on completing your first workout!", 25);
        }
    }

    /**
     * Check workout count milestone achievements
     */
    private void checkWorkoutMilestones(UserGamificationProfile userProfile, int workoutCount) {
        switch (workoutCount) {
            case 5:
                awardBadge(userProfile, "WORKOUT_5", "Getting Started",
                        "Completed 5 workouts!", 25);
                break;
            case 10:
                awardBadge(userProfile, "WORKOUT_10", "Committed",
                        "Completed 10 workouts!", 50);
                break;
            case 25:
                awardBadge(userProfile, "WORKOUT_25", "Dedicated",
                        "Completed 25 workouts!", 75);
                break;
            case 50:
                awardBadge(userProfile, "WORKOUT_50", "Fitness Enthusiast",
                        "Completed 50 workouts!", 100);
                break;
            case 100:
                awardBadge(userProfile, "WORKOUT_100", "Fitness Warrior",
                        "Completed 100 workouts!", 200);
                break;
        }
    }

    /**
     * Check streak-based achievements
     */
    private void checkStreakAchievements(UserGamificationProfile userProfile, WorkoutCompletedEvent event) {
        try {
            int currentStreak = userGamificationService.getUserCurrentStreak(event.getUserId(), "workout");

            switch (currentStreak) {
                case 3:
                    awardBadge(userProfile, "STREAK_3", "Building Momentum",
                            "3-day workout streak!", 15);
                    break;
                case 7:
                    awardBadge(userProfile, "STREAK_7", "Week Warrior",
                            "One week streak!", 35);
                    break;
                case 14:
                    awardBadge(userProfile, "STREAK_14", "Two Week Champion",
                            "Two week streak!", 75);
                    break;
                case 30:
                    awardBadge(userProfile, "STREAK_30", "Monthly Master",
                            "One month streak!", 150);
                    break;
            }
        } catch (Exception e) {
            log.warn("Failed to check streak achievements for user {}: {}", userProfile.getUserId(), e.getMessage());
        }
    }

    /**
     * Check duration-based achievements
     */
    private void checkDurationAchievements(UserGamificationProfile userProfile, WorkoutCompletedEvent event) {
        Integer duration = event.getDurationMinutes();

        if (duration != null) {
            if (duration >= 60) {
                awardBadge(userProfile, "HOUR_WORKOUT", "Hour Warrior",
                        "Worked out for over an hour!", 30);
            }

            if (duration >= 90) {
                awardBadge(userProfile, "ENDURANCE_MASTER", "Endurance Master",
                        "90+ minute workout completed!", 50);
            }
        }
    }

    /**
     * Check volume-based achievements
     */
    private void checkVolumeAchievements(UserGamificationProfile userProfile, WorkoutCompletedEvent event) {
        if (event.getTotalSets() != null && event.getTotalSets() >= 20) {
            awardBadge(userProfile, "VOLUME_CRUSHER", "Volume Crusher",
                    "Completed 20+ sets in a single workout!", 20);
        }

        if (event.getTotalReps() != null && event.getTotalReps() >= 200) {
            awardBadge(userProfile, "REP_MASTER", "Rep Master",
                    "Completed 200+ reps in a single workout!", 30);
        }
    }

    /**
     * Award a badge directly to the user profile
     */
    private void awardBadge(UserGamificationProfile userProfile, String badgeId,
                            String badgeName, String description, int bonusPoints) {
        try {
            // Check if user already has this badge
            boolean alreadyHas = userProfile.getEarnedBadges() != null &&
                    userProfile.getEarnedBadges().stream()
                            .anyMatch(badge -> badgeId.equals(badge.getBadgeId()));

            if (!alreadyHas) {
                // Create new badge
                UserBadge newBadge = UserBadge.builder()
                        .badgeId(badgeId)
                        .badgeName(badgeName)
                        .description(description)
                        .category("WORKOUT")
                        .pointsAwarded(bonusPoints)
                        .earnedAt(Instant.now())
                        .build();

                // Add badge to profile
                userProfile.addBadge(newBadge);

                // Award bonus points
                userProfile.setPoints(userProfile.getPoints() + bonusPoints);

                log.info("NEW ACHIEVEMENT: User {} earned '{}' badge with {} bonus points",
                        userProfile.getUserId(), badgeName, bonusPoints);
            }
        } catch (Exception e) {
            log.error("Failed to award badge {} to user {}: {}",
                    badgeId, userProfile.getUserId(), e.getMessage());
        }
    }
}
