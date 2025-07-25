package com.muscledia.Gamification_service.service.scheduled;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import com.muscledia.Gamification_service.event.StreakUpdatedEvent;
import com.muscledia.Gamification_service.event.publisher.GamificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Scheduled service for streak management automation.
 * Handles daily streak calculations, streak resets, and streak-based
 * notifications.
 * 
 * Senior Engineering Note: Processes streaks in batches to handle large user
 * bases
 * efficiently while maintaining accuracy and publishing relevant events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class StreakSchedulingService {

    private final UserGamificationProfileRepository userProfileRepository;
    private final GamificationEventPublisher eventPublisher;

    private static final int BATCH_SIZE = 100; // Process users in batches

    /**
     * Calculate and update streaks for all users daily at 1:30 AM
     */
    @Scheduled(cron = "${gamification.scheduling.streak-calculation.cron:0 30 1 * * ?}")
    public void calculateDailyStreaks() {
        log.info("Starting daily streak calculation for all users");

        try {
            long totalUsers = userProfileRepository.count();
            int processed = 0;
            int streaksUpdated = 0;

            // Process users in batches
            for (int page = 0; page * BATCH_SIZE < totalUsers; page++) {
                try {
                    List<UserGamificationProfile> userBatch = getUserBatch(page);
                    int batchUpdates = processStreakBatch(userBatch);

                    processed += userBatch.size();
                    streaksUpdated += batchUpdates;

                    log.debug("Processed streak batch {}: {} users, {} updates",
                            page + 1, userBatch.size(), batchUpdates);

                } catch (Exception e) {
                    log.error("Error processing streak batch {}: {}", page + 1, e.getMessage());
                }
            }

            log.info("Daily streak calculation completed. Processed: {} users, Updated: {} streaks",
                    processed, streaksUpdated);

        } catch (Exception e) {
            log.error("Error during daily streak calculation: {}", e.getMessage(), e);
        }
    }

    /**
     * Reset weekly streaks every Sunday at midnight
     */
    @Scheduled(cron = "0 0 0 ? * SUN")
    @Transactional
    public void resetWeeklyStreaks() {
        log.info("Starting weekly streak reset");

        try {
            int resetCount = 0;
            long totalUsers = userProfileRepository.count();

            for (int page = 0; page * BATCH_SIZE < totalUsers; page++) {
                try {
                    List<UserGamificationProfile> userBatch = getUserBatch(page);

                    for (UserGamificationProfile user : userBatch) {
                        if (resetWeeklyStreaksForUser(user)) {
                            resetCount++;
                        }
                    }

                } catch (Exception e) {
                    log.error("Error resetting weekly streaks for batch {}: {}", page + 1, e.getMessage());
                }
            }

            log.info("Weekly streak reset completed. Reset {} user weekly streaks", resetCount);

        } catch (Exception e) {
            log.error("Error during weekly streak reset: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for streak milestones and send notifications (hourly)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkStreakMilestones() {
        log.debug("Checking for streak milestones");

        try {
            // Find users with recent streak achievements
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

            // This would query for users who achieved streak milestones in the last hour
            // In a real implementation, you'd track streak milestone events

            log.debug("Streak milestone check completed");

        } catch (Exception e) {
            log.error("Error checking streak milestones: {}", e.getMessage(), e);
        }
    }

    /**
     * Streak leaderboard recalculation (daily at 5 AM)
     */
    @Scheduled(cron = "0 0 5 * * ?")
    @Async("backgroundProcessingExecutor")
    public void recalculateStreakLeaderboards() {
        log.info("Starting streak leaderboard recalculation");

        try {
            // Recalculate workout streak leaderboard
            recalculateStreakLeaderboard("workout");

            // Recalculate login streak leaderboard
            recalculateStreakLeaderboard("login");

            log.info("Streak leaderboard recalculation completed");

        } catch (Exception e) {
            log.error("Error during streak leaderboard recalculation: {}", e.getMessage(), e);
        }
    }

    // ===============================
    // BATCH PROCESSING METHODS
    // ===============================

    private List<UserGamificationProfile> getUserBatch(int page) {
        PageRequest pageRequest = PageRequest.of(page, BATCH_SIZE);
        return userProfileRepository.findAllByOrderByUserId(pageRequest);
    }

    @Transactional
    public int processStreakBatch(List<UserGamificationProfile> users) {
        int updatedCount = 0;

        for (UserGamificationProfile user : users) {
            try {
                if (updateUserStreaks(user)) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Error updating streaks for user {}: {}", user.getUserId(), e.getMessage());
            }
        }

        return updatedCount;
    }

    // ===============================
    // STREAK CALCULATION METHODS
    // ===============================

    private boolean updateUserStreaks(UserGamificationProfile user) {
        boolean updated = false;
        Map<String, UserGamificationProfile.StreakData> streaks = user.getStreaks();

        if (streaks == null || streaks.isEmpty()) {
            return false;
        }

        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);

        for (Map.Entry<String, UserGamificationProfile.StreakData> entry : streaks.entrySet()) {
            String streakType = entry.getKey();
            UserGamificationProfile.StreakData streakData = entry.getValue();

            if (shouldCheckStreak(streakData, yesterday)) {
                if (updateIndividualStreak(user, streakType, streakData, now)) {
                    updated = true;
                }
            }
        }

        if (updated) {
            userProfileRepository.save(user);
        }

        return updated;
    }

    private boolean shouldCheckStreak(UserGamificationProfile.StreakData streakData, Instant yesterday) {
        return streakData.getLastUpdate() == null ||
                streakData.getLastUpdate().isBefore(yesterday);
    }

    private boolean updateIndividualStreak(UserGamificationProfile user, String streakType,
            UserGamificationProfile.StreakData streakData, Instant now) {

        int previousStreak = streakData.getCurrent();

        // Check if user maintained their streak yesterday
        boolean maintainedStreak = checkStreakMaintenance(user.getUserId(), streakType, now.minus(1, ChronoUnit.DAYS));

        if (!maintainedStreak && streakData.getCurrent() > 0) {
            // Streak broken
            streakData.setCurrent(0);
            streakData.setLastUpdate(now);

            // Publish streak broken event
            publishStreakEvent(user, streakType, streakData, previousStreak, "RESET");

            log.debug("Reset {} streak for user {}: was {}", streakType, user.getUserId(), previousStreak);
            return true;
        }

        return false;
    }

    private boolean checkStreakMaintenance(Long userId, String streakType, Instant date) {
        // This is a simplified check - in a real implementation, you'd query
        // activity data to determine if the user maintained their streak

        switch (streakType.toLowerCase()) {
            case "workout":
                return checkWorkoutActivity(userId, date);
            case "login":
                return checkLoginActivity(userId, date);
            default:
                return false;
        }
    }

    private boolean checkWorkoutActivity(Long userId, Instant date) {
        // In a real implementation, this would query workout completion data
        // For now, return a simplified check
        return false; // Assume no activity to demonstrate streak reset
    }

    private boolean checkLoginActivity(Long userId, Instant date) {
        // In a real implementation, this would query login/session data
        return false; // Assume no activity to demonstrate streak reset
    }

    private boolean resetWeeklyStreaksForUser(UserGamificationProfile user) {
        Map<String, UserGamificationProfile.StreakData> streaks = user.getStreaks();

        if (streaks == null || streaks.isEmpty()) {
            return false;
        }

        boolean updated = false;

        // Reset weekly streaks (if you have any)
        for (Map.Entry<String, UserGamificationProfile.StreakData> entry : streaks.entrySet()) {
            String streakType = entry.getKey();

            // Only reset weekly streak types
            if (isWeeklyStreakType(streakType)) {
                UserGamificationProfile.StreakData streakData = entry.getValue();
                int previousStreak = streakData.getCurrent();

                streakData.setCurrent(0);
                streakData.setLastUpdate(Instant.now());

                publishStreakEvent(user, streakType, streakData, previousStreak, "RESET");
                updated = true;
            }
        }

        if (updated) {
            userProfileRepository.save(user);
        }

        return updated;
    }

    private boolean isWeeklyStreakType(String streakType) {
        // Define which streak types reset weekly
        return streakType.toLowerCase().contains("weekly") ||
                streakType.toLowerCase().contains("week");
    }

    // ===============================
    // LEADERBOARD METHODS
    // ===============================

    private void recalculateStreakLeaderboard(String streakType) {
        try {
            PageRequest topStreaks = PageRequest.of(0, 100);
            List<UserGamificationProfile> topUsers = userProfileRepository.findTopUsersByStreak(streakType, topStreaks);

            log.info("Recalculated {} streak leaderboard: {} users", streakType, topUsers.size());

            // In a real implementation, you'd update cached leaderboard data
            // and possibly publish leaderboard update events

        } catch (Exception e) {
            log.error("Error recalculating {} streak leaderboard: {}", streakType, e.getMessage());
        }
    }

    // ===============================
    // EVENT PUBLISHING
    // ===============================

    private void publishStreakEvent(UserGamificationProfile user, String streakType,
            UserGamificationProfile.StreakData streakData,
            int previousStreak, String action) {
        try {
            StreakUpdatedEvent event = new StreakUpdatedEvent();
            event.setUserId(user.getUserId());
            event.setStreakType(streakType);
            event.setCurrentStreak(streakData.getCurrent());
            event.setPreviousStreak(previousStreak);
            event.setLongestStreak(streakData.getLongest());
            event.setStreakAction(action);
            event.setTriggeringActivity("scheduled_calculation");

            eventPublisher.publishGamificationEvent(event);

        } catch (Exception e) {
            log.error("Error publishing streak event for user {}: {}", user.getUserId(), e.getMessage());
        }
    }
}