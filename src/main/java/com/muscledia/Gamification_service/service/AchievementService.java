package com.muscledia.Gamification_service.service;


import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.model.UserBadge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing user achievements and automatic badge awarding
 *
 * This service implements the core achievement logic for the user story:
 * "Earn achievements automatically when completing specific actions"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final UserGamificationService userGamificationService;
    private final BadgeService badgeService;

    /**
     * Process workout completion and check for achievements
     *
     * IMPLEMENTS USER STORY ACCEPTANCE CRITERIA:
     * - Check if workout satisfies achievement criteria (e.g., first workout)
     * - Update user record to show new achievement
     */
    @Transactional
    public void processWorkoutAchievements(WorkoutCompletedEvent event) {
        log.info("Processing achievements for user {} from workout {}",
                event.getUserId(), event.getWorkoutId());

        // Get or create user profile
        UserGamificationProfile userProfile = userGamificationService.createOrGetUserProfile(event.getUserId());

        // Check various achievement types
        checkFirstWorkoutAchievement(event, userProfile);
        checkWorkoutCountMilestones(event, userProfile);
        checkConsistencyAchievements(event, userProfile);
        checkIntensityAchievements(event, userProfile);
        checkDurationAchievements(event, userProfile);
        checkVolumeAchievements(event, userProfile);
    }

    /**
     * Check and award "First Workout" achievement
     * Classic example from the user story
     */
    private void checkFirstWorkoutAchievement(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        // Use actual workout count if available, otherwise estimate from points
        int workoutCount = userProfile.getTotalWorkoutsCompleted() != null ?
                userProfile.getTotalWorkoutsCompleted() :
                userProfile.getPoints() / 50;

        if (workoutCount <= 1) { // This is their first workout
            log.info("FIRST WORKOUT ACHIEVEMENT: User {} completed their first workout!",
                    event.getUserId());

            awardAchievementBadge(userProfile, "FIRST_WORKOUT", "First Steps",
                    "Completed your first workout! The journey begins.", 10);
        }
    }

    /**
     * Check workout count milestone achievements
     */
    private void checkWorkoutCountMilestones(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        // Use actual workout count if available
        int workoutCount = userProfile.getTotalWorkoutsCompleted() != null ?
                userProfile.getTotalWorkoutsCompleted() :
                (userProfile.getPoints() / 50) + 1;

        // Define milestones
        Map<Integer, String[]> milestones = new HashMap<>();
        milestones.put(5, new String[]{"WORKOUT_5", "Getting Started", "Completed 5 workouts"});
        milestones.put(10, new String[]{"WORKOUT_10", "Committed", "Completed 10 workouts"});
        milestones.put(25, new String[]{"WORKOUT_25", "Dedicated", "Completed 25 workouts"});
        milestones.put(50, new String[]{"WORKOUT_50", "Fitness Enthusiast", "Completed 50 workouts"});
        milestones.put(100, new String[]{"WORKOUT_100", "Fitness Warrior", "Completed 100 workouts"});

        for (Map.Entry<Integer, String[]> milestone : milestones.entrySet()) {
            if (workoutCount == milestone.getKey()) {
                String[] badgeInfo = milestone.getValue();
                log.info("MILESTONE ACHIEVEMENT: User {} reached {} workouts!",
                        event.getUserId(), milestone.getKey());

                awardAchievementBadge(userProfile, badgeInfo[0], badgeInfo[1], badgeInfo[2],
                        milestone.getKey() * 5); // Bonus points
                break;
            }
        }
    }

    /**
     * Check consistency/streak achievements
     */
    private void checkConsistencyAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        // Check current workout streak
        int currentStreak = userGamificationService.getUserCurrentStreak(event.getUserId(), "workout");

        if (currentStreak == 7) {
            awardAchievementBadge(userProfile, "WEEK_STREAK", "Week Warrior",
                    "Worked out every day for a week!", 25);
        } else if (currentStreak == 30) {
            awardAchievementBadge(userProfile, "MONTH_STREAK", "Monthly Master",
                    "30-day workout streak achieved!", 100);
        }
    }

    /**
     * Check intensity-based achievements
     */
    private void checkIntensityAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        double intensity = event.getIntensityScore();

        if (intensity >= 3.0) {
            awardAchievementBadge(userProfile, "HIGH_INTENSITY", "Intensity Monster",
                    "Completed a high-intensity workout!", 15);
        }

        if (intensity >= 5.0) {
            awardAchievementBadge(userProfile, "EXTREME_INTENSITY", "Beast Mode",
                    "Extreme intensity workout completed!", 30);
        }
    }

    /**
     * Check duration-based achievements
     */
    private void checkDurationAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        Integer duration = event.getDurationMinutes();

        if (duration != null) {
            if (duration >= 60) {
                awardAchievementBadge(userProfile, "HOUR_WARRIOR", "Hour Warrior",
                        "Worked out for over an hour!", 20);
            }

            if (duration >= 120) {
                awardAchievementBadge(userProfile, "ENDURANCE_KING", "Endurance King",
                        "Epic 2+ hour workout completed!", 50);
            }
        }
    }


    /**
     * FIXED: Award an achievement badge to the user using BadgeService
     */
    private void awardAchievementBadge(UserGamificationProfile userProfile, String badgeId,
                                       String badgeName, String description, int bonusPoints) {
        try {
            // Check if user already has this badge
            boolean alreadyHas = userProfile.getEarnedBadges().stream()
                    .anyMatch(badge -> badgeId.equals(badge.getBadgeId()));

            if (!alreadyHas) {
                // FIXED: Use BadgeService to award the badge
                try {
                    badgeService.awardBadge(userProfile.getUserId(), badgeId);
                    log.info("NEW ACHIEVEMENT: User {} earned '{}' badge via BadgeService",
                            userProfile.getUserId(), badgeName);
                } catch (Exception e) {
                    // If BadgeService fails, manually add to profile as fallback
                    log.warn("BadgeService failed, adding badge manually: {}", e.getMessage());
                    UserBadge newBadge = UserBadge.builder()
                            .badgeId(badgeId)
                            .badgeName(badgeName)
                            .description(description)
                            .earnedAt(Instant.now())
                            .pointsAwarded(bonusPoints)
                            .build();

                    userProfile.addBadge(newBadge);
                    userGamificationService.createOrGetUserProfile(userProfile.getUserId()); // Save profile
                }

                // Award bonus points for the achievement
                if (bonusPoints > 0) {
                    userGamificationService.updateUserPoints(userProfile.getUserId(), bonusPoints);
                }

                log.info("ACHIEVEMENT AWARDED: User {} earned '{}' badge with {} bonus points",
                        userProfile.getUserId(), badgeName, bonusPoints);
            } else {
                log.debug("User {} already has badge {}, skipping", userProfile.getUserId(), badgeId);
            }
        } catch (Exception e) {
            log.error("Failed to award achievement badge {} to user {}: {}",
                    badgeId, userProfile.getUserId(), e.getMessage());
        }
    }


    /**
     * FIXED: Check volume-based achievements (missing method)
     */
    private void checkVolumeAchievements(WorkoutCompletedEvent event, UserGamificationProfile userProfile) {
        // Check total sets achievement
        if (event.getTotalSets() != null && event.getTotalSets() >= 20) {
            awardAchievementBadge(userProfile, "VOLUME_CRUSHER", "Volume Crusher",
                    "Completed 20+ sets in a single workout!", 15);
        }

        // Check total reps achievement
        if (event.getTotalReps() != null && event.getTotalReps() >= 200) {
            awardAchievementBadge(userProfile, "REP_MASTER", "Rep Master",
                    "Completed 200+ reps in a single workout!", 25);
        }

        // Check for extreme volume
        if (event.getTotalSets() != null && event.getTotalSets() >= 50) {
            awardAchievementBadge(userProfile, "EXTREME_VOLUME", "Volume Beast",
                    "Completed 50+ sets in a single workout!", 40);
        }
    }
}
