package com.muscledia.Gamification_service.event.handler;

import com.muscledia.Gamification_service.event.PersonalRecordEvent;
import com.muscledia.Gamification_service.service.BadgeService;
import com.muscledia.Gamification_service.service.UserGamificationService;
import com.muscledia.Gamification_service.event.publisher.GamificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for personal record events.
 * Processes PR achievements and triggers high-value badge evaluations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersonalRecordEventHandler {

    private final BadgeService badgeService;
    private final UserGamificationService userGamificationService;
    private final GamificationEventPublisher eventPublisher;

    @Transactional
    public void handlePersonalRecord(PersonalRecordEvent event) {
        log.info("Processing personal record for user {} - {} {} {}",
                event.getUserId(), event.getNewValue(), event.getUnit(), event.getExerciseName());

        try {
            // Award substantial points for PR
            awardPersonalRecordPoints(event);

            // Evaluate PR-specific badges
            evaluatePersonalRecordBadges(event);

            // Check for milestone achievements
            checkPersonalRecordMilestones(event);

            log.info("Successfully processed personal record for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Error processing personal record for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    private void awardPersonalRecordPoints(PersonalRecordEvent event) {
        try {
            int basePoints = 100; // Base points for any PR
            int improvementBonus = calculateImprovementBonus(event);
            int milestoneBonus = event.isMilestonePR() ? 50 : 0;

            int totalPoints = basePoints + improvementBonus + milestoneBonus;

            userGamificationService.updateUserPoints(event.getUserId(), totalPoints);

            log.info("Awarded {} points to user {} for personal record",
                    totalPoints, event.getUserId());

        } catch (Exception e) {
            log.error("Error awarding PR points to user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private void evaluatePersonalRecordBadges(PersonalRecordEvent event) {
        try {
            Map<String, Object> prStats = buildPersonalRecordStats(event);

            var eligibleBadges = badgeService.getEligibleBadges(event.getUserId(), prStats);

            for (var badge : eligibleBadges) {
                try {
                    badgeService.awardBadge(event.getUserId(), badge.getBadgeId());
                    log.info("Awarded PR badge {} to user {}",
                            badge.getName(), event.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to award PR badge {} to user {}: {}",
                            badge.getBadgeId(), event.getUserId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error evaluating PR badges for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private void checkPersonalRecordMilestones(PersonalRecordEvent event) {
        try {
            if (event.isMilestonePR()) {
                log.info("User {} achieved a milestone PR: {} {} {}",
                        event.getUserId(), event.getNewValue(), event.getUnit(), event.getExerciseName());
            }

            String significance = event.getSignificanceLevel();
            if ("MAJOR".equals(significance) || "SIGNIFICANT".equals(significance)) {
                log.info("User {} achieved a {} personal record improvement",
                        event.getUserId(), significance.toLowerCase());
            }

        } catch (Exception e) {
            log.error("Error checking PR milestones for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }

    private int calculateImprovementBonus(PersonalRecordEvent event) {
        double improvement = event.getImprovementPercentage();

        if (improvement >= 50)
            return 100; // 50%+ improvement
        if (improvement >= 25)
            return 50; // 25%+ improvement
        if (improvement >= 10)
            return 25; // 10%+ improvement
        return 10; // Any improvement
    }

    private Map<String, Object> buildPersonalRecordStats(PersonalRecordEvent event) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("exerciseName", event.getExerciseName());
        stats.put("recordType", event.getRecordType());
        stats.put("newValue", event.getNewValue());
        stats.put("previousValue", event.getPreviousValue());
        stats.put("unit", event.getUnit());
        stats.put("improvementPercentage", event.getImprovementPercentage());
        stats.put("isMilestonePR", event.isMilestonePR());
        stats.put("significanceLevel", event.getSignificanceLevel());

        return stats;
    }
}