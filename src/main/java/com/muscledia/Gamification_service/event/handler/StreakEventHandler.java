package com.muscledia.Gamification_service.event.handler;

import com.muscledia.Gamification_service.event.StreakUpdatedEvent;
import com.muscledia.Gamification_service.service.BadgeService;
import com.muscledia.Gamification_service.service.UserGamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for streak update events.
 * Processes streak milestones and triggers streak-based badges.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreakEventHandler {

    private final BadgeService badgeService;
    private final UserGamificationService userGamificationService;

    @Transactional
    public void handleStreakUpdate(StreakUpdatedEvent event) {
        log.info("Processing streak update for user {} - {} streak: {}",
                event.getUserId(), event.getStreakType(), event.getCurrentStreak());

        try {
            // Award points for streak milestones
            awardStreakPoints(event);

            // Evaluate streak-based badges
            evaluateStreakBadges(event);

            // Handle streak achievements
            handleStreakAchievements(event);

            log.info("Successfully processed streak update for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Error processing streak update for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    private void awardStreakPoints(StreakUpdatedEvent event) {
        try {
            if (!"INCREASED".equals(event.getStreakAction())) {
                return; // Only award points for streak increases
            }

            int points = 0;

            if (event.isMilestone()) {
                points = calculateMilestonePoints(event);
            } else if (event.isNewRecord()) {
                points = 50; // New personal best
            } else if ("INCREASED".equals(event.getStreakAction())) {
                points = 5; // Small bonus for any streak increase
            }

            if (points > 0) {
                userGamificationService.updateUserPoints(event.getUserId(), points);
                log.info("Awarded {} points to user {} for streak achievement",
                        points, event.getUserId());
            }

        } catch (Exception e) {
            log.error("Error awarding streak points to user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private void evaluateStreakBadges(StreakUpdatedEvent event) {
        try {
            // Only evaluate badges for significant streak events
            if (!event.isMilestone() && !event.isNewRecord()) {
                return;
            }

            Map<String, Object> streakStats = buildStreakStats(event);

            var eligibleBadges = badgeService.getEligibleBadges(event.getUserId(), streakStats);

            for (var badge : eligibleBadges) {
                try {
                    badgeService.awardBadge(event.getUserId(), badge.getBadgeId());
                    log.info("Awarded streak badge {} to user {}",
                            badge.getName(), event.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to award streak badge {} to user {}: {}",
                            badge.getBadgeId(), event.getUserId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error evaluating streak badges for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private void handleStreakAchievements(StreakUpdatedEvent event) {
        try {
            String significance = event.getSignificanceLevel();

            switch (significance) {
                case "RECORD" -> log.info("User {} achieved a new {} streak record: {} days!",
                        event.getUserId(), event.getStreakType(), event.getCurrentStreak());
                case "MILESTONE" -> log.info("User {} reached a {} streak milestone: {} days",
                        event.getUserId(), event.getStreakType(), event.getCurrentStreak());
                case "SIGNIFICANT_LOSS" -> log.warn("User {} lost a significant {} streak: was {} days",
                        event.getUserId(), event.getStreakType(), event.getPreviousStreak());
            }

        } catch (Exception e) {
            log.error("Error handling streak achievements for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private int calculateMilestonePoints(StreakUpdatedEvent event) {
        Integer currentStreak = event.getCurrentStreak();
        if (currentStreak == null)
            return 0;

        return switch (event.getStreakType().toLowerCase()) {
            case "workout" -> calculateWorkoutStreakPoints(currentStreak);
            case "login" -> calculateLoginStreakPoints(currentStreak);
            default -> calculateGenericStreakPoints(currentStreak);
        };
    }

    private int calculateWorkoutStreakPoints(int streak) {
        if (streak >= 100)
            return 500; // 100+ day streak
        if (streak >= 60)
            return 300; // 60+ day streak
        if (streak >= 30)
            return 200; // 30+ day streak
        if (streak >= 14)
            return 100; // 2 week streak
        if (streak >= 7)
            return 50; // 1 week streak
        return 25; // Other milestones
    }

    private int calculateLoginStreakPoints(int streak) {
        if (streak >= 100)
            return 200; // 100+ day login streak
        if (streak >= 60)
            return 150; // 60+ day login streak
        if (streak >= 30)
            return 100; // 30+ day login streak
        if (streak >= 10)
            return 50; // 10+ day login streak
        return 25; // Other milestones
    }

    private int calculateGenericStreakPoints(int streak) {
        if (streak >= 50)
            return 100; // 50+ day generic streak
        if (streak >= 25)
            return 75; // 25+ day generic streak
        if (streak >= 10)
            return 50; // 10+ day generic streak
        if (streak >= 5)
            return 25; // 5+ day generic streak
        return 10; // Other milestones
    }

    private Map<String, Object> buildStreakStats(StreakUpdatedEvent event) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("streakType", event.getStreakType());
        stats.put("currentStreak", event.getCurrentStreak());
        stats.put("previousStreak", event.getPreviousStreak());
        stats.put("longestStreak", event.getLongestStreak());
        stats.put("streakAction", event.getStreakAction());
        stats.put("isMilestone", event.isMilestone());
        stats.put("isNewRecord", event.isNewRecord());
        stats.put("significanceLevel", event.getSignificanceLevel());
        stats.put("streakDelta", event.getStreakDelta());

        return stats;
    }
}