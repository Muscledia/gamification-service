package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import com.muscledia.Gamification_service.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * CURATED BADGES: 12 meaningful achievements
 * Signal over Noise - each badge is special
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class BadgeInitializationService implements CommandLineRunner {

    private final BadgeRepository badgeRepository;

    @Override
    public void run(String... args) {
        long count = badgeRepository.count();

        if (count == 0) {
            log.info("Initializing 12 curated badges...");
            initializeCuratedBadges();
        } else {
            log.info("Badges already initialized: {}", count);
        }
    }

    private void initializeCuratedBadges() {
        // WORKOUT MILESTONES (5 badges)
        createBadge("FIRST_WORKOUT", "First Steps",
                "Complete your first workout",
                BadgeType.EXERCISE, BadgeCriteriaType.WORKOUT_COUNT, 1, 25);

        createBadge("WORKOUT_10", "Dedicated",
                "Complete 10 workouts",
                BadgeType.EXERCISE, BadgeCriteriaType.WORKOUT_COUNT, 10, 100);

        createBadge("WORKOUT_50", "Committed",
                "Complete 50 workouts",
                BadgeType.EXERCISE, BadgeCriteriaType.WORKOUT_COUNT, 50, 400);

        createBadge("WORKOUT_100", "Century Club",
                "Complete 100 workouts",
                BadgeType.CHAMPION, BadgeCriteriaType.WORKOUT_COUNT, 100, 1000);

        createBadge("WORKOUT_500", "Iron Dedication",
                "Complete 500 workouts",
                BadgeType.CHAMPION, BadgeCriteriaType.WORKOUT_COUNT, 500, 5000);

        // STREAK ACHIEVEMENTS (3 badges)
        createBadge("STREAK_7", "Week Warrior",
                "Maintain 7-day workout streak",
                BadgeType.STREAK, BadgeCriteriaType.WORKOUT_STREAK, 7, 150);

        createBadge("STREAK_30", "Monthly Momentum",
                "Maintain 30-day workout streak",
                BadgeType.STREAK, BadgeCriteriaType.WORKOUT_STREAK, 30, 500);

        createBadge("STREAK_100", "Unstoppable",
                "Maintain 100-day workout streak",
                BadgeType.CHAMPION, BadgeCriteriaType.WORKOUT_STREAK, 100, 2000);

        // PERSONAL RECORDS (3 badges)
        createBadge("PR_FIRST", "Record Breaker",
                "Set your first personal record",
                BadgeType.PR, BadgeCriteriaType.PERSONAL_RECORD, 1, 50);

        createBadge("PR_10", "Progress Machine",
                "Set 10 personal records",
                BadgeType.PR, BadgeCriteriaType.PERSONAL_RECORD, 10, 300);

        createBadge("PR_50", "Strength Legend",
                "Set 50 personal records",
                BadgeType.CHAMPION, BadgeCriteriaType.PERSONAL_RECORD, 50, 1500);

        // LEVEL MILESTONE (1 badge)
        createBadge("LEVEL_50", "Elite Athlete",
                "Reach level 50",
                BadgeType.CHAMPION, BadgeCriteriaType.LEVEL_REACHED, 50, 2500);

        log.info("✅ Initialized 12 curated badges");
    }

    private void createBadge(String id, String name, String description,
                             BadgeType type, BadgeCriteriaType criteriaType,
                             int targetValue, int points) {
        Badge badge = new Badge();
        badge.setBadgeId(id);
        badge.setName(name);
        badge.setDescription(description);
        badge.setBadgeType(type);
        badge.setCriteriaType(criteriaType);
        badge.setCriteriaParams(Map.of("targetValue", targetValue));
        badge.setPointsAwarded(points);
        badge.setCreatedAt(Instant.now());

        badgeRepository.save(badge);
    }
}