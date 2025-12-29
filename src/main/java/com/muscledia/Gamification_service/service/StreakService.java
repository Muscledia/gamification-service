package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.StreakUpdatedEvent;
import com.muscledia.Gamification_service.event.publisher.EventPublisher;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final UserGamificationProfileRepository userProfileRepository;
    private final LeaderboardChangeDetectionService leaderboardDetection;
    private final EventPublisher eventPublisher;

    /**
     * Update both weekly and monthly streaks when a workout is completed
     * RETURNS: StreakUpdateResult for celebration logging
     */
    @Transactional
    public StreakUpdateResult updateStreaks(Long userId, Instant workoutCompletedAt) {
        log.info("Updating streaks for user {} at {}", userId, workoutCompletedAt);

        UserGamificationProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> createNewProfile(userId));

        // Capture old values for leaderboard change detection
        int oldWeeklyStreak = profile.getWeeklyStreak() != null ? profile.getWeeklyStreak() : 0;
        int oldMonthlyStreak = profile.getMonthlyStreak() != null ? profile.getMonthlyStreak() : 0;

        // Update weekly streak
        updateWeeklyStreak(profile, workoutCompletedAt);

        // Update monthly streak
        updateMonthlyStreak(profile, workoutCompletedAt);

        // Update rest days
        profile.setRestDaysSinceLastWorkout(0);
        profile.setLastWorkoutDate(workoutCompletedAt);
        profile.setLastUpdated(Instant.now());

        userProfileRepository.save(profile);

        // Get new values
        int newWeeklyStreak = profile.getWeeklyStreak() != null ? profile.getWeeklyStreak() : 0;
        int newMonthlyStreak = profile.getMonthlyStreak() != null ? profile.getMonthlyStreak() : 0;

        // Check for leaderboard changes
        leaderboardDetection.checkWeeklyStreakRankChange(userId, oldWeeklyStreak, newWeeklyStreak);
        leaderboardDetection.checkMonthlyStreakRankChange(userId, oldMonthlyStreak, newMonthlyStreak);

        // ⬅️ PUBLISH STREAK EVENTS
        if (oldWeeklyStreak != newWeeklyStreak) {
            publishStreakEvent(userId, "WEEKLY", oldWeeklyStreak, newWeeklyStreak,
                    profile.getLongestWeeklyStreak());
        }

        if (oldMonthlyStreak != newMonthlyStreak) {
            publishStreakEvent(userId, "MONTHLY", oldMonthlyStreak, newMonthlyStreak,
                    profile.getLongestMonthlyStreak());
        }

        log.info("Updated streaks for user {} - Weekly: {}, Monthly: {}",
                userId, newWeeklyStreak, newMonthlyStreak);

        return StreakUpdateResult.builder()
                .userId(userId)
                .weeklyStreak(newWeeklyStreak)
                .monthlyStreak(newMonthlyStreak)
                .weeklyStreakIncreased(newWeeklyStreak > oldWeeklyStreak)
                .monthlyStreakIncreased(newMonthlyStreak > oldMonthlyStreak)
                .isNewMilestone(isStreakMilestone(newWeeklyStreak) || isStreakMilestone(newMonthlyStreak))
                .currentStreak(newWeeklyStreak)
                .continues(newWeeklyStreak > oldWeeklyStreak)
                .build();
    }

    /**
     * Publish streak updated event
     */
    private void publishStreakEvent(Long userId, String streakType, int oldStreak,
                                    int newStreak, int longestStreak) {
        try {
            String action = determineStreakAction(oldStreak, newStreak);

            StreakUpdatedEvent event = StreakUpdatedEvent.builder()
                    .userId(userId)
                    .streakType(streakType)
                    .currentStreak(newStreak)
                    .previousStreak(oldStreak)
                    .longestStreak(longestStreak)
                    .streakAction(action)
                    .triggeringActivity("WORKOUT_COMPLETED")
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishStreakUpdated(event);

            log.debug("Published {} streak event for user {}: {} → {}",
                    streakType, userId, oldStreak, newStreak);

        } catch (Exception e) {
            log.error("Failed to publish streak event for user {}: {}", userId, e.getMessage());
            // Don't fail the transaction if event publishing fails
        }
    }

    /**
     * Determine what action occurred with the streak
     */
    private String determineStreakAction(int oldStreak, int newStreak) {
        if (newStreak == 0) return "RESET";
        if (newStreak > oldStreak) return "INCREASED";
        if (newStreak < oldStreak) return "DECREASED";
        return "MAINTAINED";
    }

    /**
     * Check if streak value is a milestone
     */
    private boolean isStreakMilestone(int streak) {
        return streak == 7 || streak == 14 || streak == 30 ||
                streak == 60 || streak == 100 ||
                (streak > 100 && streak % 50 == 0);
    }

    /**
     * Update weekly streak (at least 1 workout per week)
     */
    private void updateWeeklyStreak(UserGamificationProfile profile, Instant workoutDate) {
        Instant currentWeekStart = getWeekStart(workoutDate);
        Instant previousWeekStart = profile.getCurrentWeekStartDate();
        Instant lastWorkout = profile.getLastWorkoutDate();

        // First workout ever
        if (lastWorkout == null) {
            profile.setWeeklyStreak(1);
            profile.setLongestWeeklyStreak(1);
            profile.setCurrentWeekStartDate(currentWeekStart);
            log.info("Initialized weekly streak for user {}", profile.getUserId());
            return;
        }

        // Same week - no change needed
        if (currentWeekStart.equals(previousWeekStart)) {
            log.debug("Same week workout for user {}", profile.getUserId());
            return;
        }

        // Different week - check if consecutive
        if (isConsecutiveWeek(previousWeekStart, currentWeekStart)) {
            incrementWeeklyStreak(profile);
        } else {
            resetWeeklyStreak(profile);
        }

        profile.setCurrentWeekStartDate(currentWeekStart);
    }

    /**
     * Update monthly streak (at least 1 workout per month)
     */
    private void updateMonthlyStreak(UserGamificationProfile profile, Instant workoutDate) {
        Instant currentMonthStart = getMonthStart(workoutDate);
        Instant previousMonthStart = profile.getCurrentMonthStartDate();
        Instant lastWorkout = profile.getLastWorkoutDate();

        // First workout ever
        if (lastWorkout == null) {
            profile.setMonthlyStreak(1);
            profile.setLongestMonthlyStreak(1);
            profile.setCurrentMonthStartDate(currentMonthStart);
            log.info("Initialized monthly streak for user {}", profile.getUserId());
            return;
        }

        // Same month - no change needed
        if (currentMonthStart.equals(previousMonthStart)) {
            log.debug("Same month workout for user {}", profile.getUserId());
            return;
        }

        // Different month - check if consecutive
        if (isConsecutiveMonth(previousMonthStart, currentMonthStart)) {
            incrementMonthlyStreak(profile);
        } else {
            resetMonthlyStreak(profile);
        }

        profile.setCurrentMonthStartDate(currentMonthStart);
    }

    /**
     * Calculate and update rest days since last workout
     */
    @Transactional
    public void updateRestDays(Long userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            if (profile.getLastWorkoutDate() != null) {
                long daysSinceLastWorkout = ChronoUnit.DAYS.between(
                        profile.getLastWorkoutDate().truncatedTo(ChronoUnit.DAYS),
                        Instant.now().truncatedTo(ChronoUnit.DAYS)
                );

                profile.setRestDaysSinceLastWorkout((int) daysSinceLastWorkout);
                profile.setLastUpdated(Instant.now());
                userProfileRepository.save(profile);

                log.debug("Updated rest days for user {}: {} days", userId, daysSinceLastWorkout);
            }
        });
    }

    /**
     * Get week start date (Monday at 00:00)
     */
    private Instant getWeekStart(Instant date) {
        ZonedDateTime zdt = date.atZone(ZoneId.systemDefault());
        ZonedDateTime weekStart = zdt.with(DayOfWeek.MONDAY)
                .truncatedTo(ChronoUnit.DAYS);
        return weekStart.toInstant();
    }

    /**
     * Get month start date (1st day at 00:00)
     */
    private Instant getMonthStart(Instant date) {
        ZonedDateTime zdt = date.atZone(ZoneId.systemDefault());
        ZonedDateTime monthStart = zdt.withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
        return monthStart.toInstant();
    }

    /**
     * Check if two week start dates are consecutive
     */
    private boolean isConsecutiveWeek(Instant previousWeekStart, Instant currentWeekStart) {
        if (previousWeekStart == null) {
            return false;
        }
        Instant nextExpectedWeek = previousWeekStart.plus(7, ChronoUnit.DAYS);
        return currentWeekStart.equals(nextExpectedWeek);
    }

    /**
     * Check if two month start dates are consecutive
     */
    private boolean isConsecutiveMonth(Instant previousMonthStart, Instant currentMonthStart) {
        if (previousMonthStart == null) {
            return false;
        }

        ZonedDateTime previous = previousMonthStart.atZone(ZoneId.systemDefault());
        ZonedDateTime current = currentMonthStart.atZone(ZoneId.systemDefault());

        YearMonth previousYearMonth = YearMonth.from(previous);
        YearMonth currentYearMonth = YearMonth.from(current);
        YearMonth nextExpectedMonth = previousYearMonth.plusMonths(1);

        return currentYearMonth.equals(nextExpectedMonth);
    }

    /**
     * Increment weekly streak
     */
    private void incrementWeeklyStreak(UserGamificationProfile profile) {
        int newStreak = profile.getWeeklyStreak() + 1;
        profile.setWeeklyStreak(newStreak);

        if (newStreak > profile.getLongestWeeklyStreak()) {
            profile.setLongestWeeklyStreak(newStreak);
            log.info("New longest weekly streak for user {}: {} weeks",
                    profile.getUserId(), newStreak);
        }
    }

    /**
     * Reset weekly streak to 1
     */
    private void resetWeeklyStreak(UserGamificationProfile profile) {
        log.info("Weekly streak broken for user {}, resetting from {} to 1",
                profile.getUserId(), profile.getWeeklyStreak());
        profile.setWeeklyStreak(1);
    }

    /**
     * Increment monthly streak
     */
    private void incrementMonthlyStreak(UserGamificationProfile profile) {
        int newStreak = profile.getMonthlyStreak() + 1;
        profile.setMonthlyStreak(newStreak);

        if (newStreak > profile.getLongestMonthlyStreak()) {
            profile.setLongestMonthlyStreak(newStreak);
            log.info("New longest monthly streak for user {}: {} months",
                    profile.getUserId(), newStreak);
        }
    }

    /**
     * Reset monthly streak to 1
     */
    private void resetMonthlyStreak(UserGamificationProfile profile) {
        log.info("Monthly streak broken for user {}, resetting from {} to 1",
                profile.getUserId(), profile.getMonthlyStreak());
        profile.setMonthlyStreak(1);
    }

    /**
     * Create new profile for user
     */
    private UserGamificationProfile createNewProfile(Long userId) {
        UserGamificationProfile profile = UserGamificationProfile.builder()
                .userId(userId)
                .weeklyStreak(0)
                .longestWeeklyStreak(0)
                .monthlyStreak(0)
                .longestMonthlyStreak(0)
                .restDaysSinceLastWorkout(0)
                .points(0)
                .level(1)
                .totalWorkoutsCompleted(0)
                .profileCreatedAt(Instant.now())
                .build();
        profile.initializeDefaults();
        return profile;
    }

    // ==================== RESULT CLASS ====================

    /**
     * Result object for streak updates
     * Used for celebration logging in WorkoutEventHandler
     */
    @Data
    @Builder
    public static class StreakUpdateResult {
        private Long userId;
        private Integer weeklyStreak;
        private Integer monthlyStreak;
        private Boolean weeklyStreakIncreased;
        private Boolean monthlyStreakIncreased;
        private Boolean isNewMilestone;

        // For backward compatibility with WorkoutEventHandler
        private Integer currentStreak;  // Maps to weeklyStreak
        private Boolean continues;      // Maps to weeklyStreakIncreased
    }
}