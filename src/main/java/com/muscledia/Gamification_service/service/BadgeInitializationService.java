package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import com.muscledia.Gamification_service.model.enums.BadgeType;
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
 * Initializes default badges in the database on application startup.
 * Only runs if badges collection is empty.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class BadgeInitializationService implements CommandLineRunner {

    private final BadgeRepository badgeRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing badges...");

        // Only create badges if they don't exist
        if (badgeRepository.count() > 0) {
            log.info("Badges already initialized. Count: {}", badgeRepository.count());
            return;
        }

        // Initialize all badge categories
        createWelcomeBadges();
        createWorkoutCountBadges();
        createStreakBadges();
        createPRBadges();
        createLevelBadges();
        createWorkoutDurationBadges();
        createWeightLiftedBadges();
        createExerciseVarietyBadges();
        createConsistencyBadges();
        createChampionBadges();

        long totalBadges = badgeRepository.count();
        log.info("Badge initialization complete! Total badges: {}", totalBadges);
    }

    // ===========================================
    // WELCOME & ONBOARDING BADGES
    // ===========================================

    private void createWelcomeBadges() {
        log.info("Creating welcome badges...");

        createBadge(
                "WELCOME",
                "Welcome to Muscledia!",
                "Welcome to your fitness journey!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 0),
                10
        );
    }

    // ===========================================
    // WORKOUT COUNT BADGES
    // ===========================================

    private void createWorkoutCountBadges() {
        log.info("Creating workout count badges...");

        createBadge(
                "FIRST_WORKOUT",
                "First Steps",
                "Congratulations on completing your first workout!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 1),
                25
        );

        createBadge(
                "WORKOUT_5",
                "Getting Started",
                "Completed 5 workouts",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 5),
                50
        );

        createBadge(
                "WORKOUT_10",
                "Dedicated",
                "Completed 10 workouts",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 10),
                100
        );

        createBadge(
                "WORKOUT_25",
                "Committed",
                "Completed 25 workouts",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 25),
                200
        );

        createBadge(
                "WORKOUT_50",
                "Fitness Enthusiast",
                "Completed 50 workouts",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 50),
                400
        );

        createBadge(
                "WORKOUT_100",
                "Century Club",
                "Completed 100 workouts!",
                BadgeType.CHAMPION,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 100),
                1000
        );
    }

    // ===========================================
    // STREAK BADGES
    // ===========================================

    private void createStreakBadges() {
        log.info("Creating streak badges...");

        createBadge(
                "STREAK_3",
                "Consistency Starter",
                "Complete 3 consecutive days of workouts",
                BadgeType.STREAK,
                BadgeCriteriaType.WORKOUT_STREAK,
                Map.of("targetValue", 3),
                50
        );

        createBadge(
                "STREAK_5",
                "Workout Warrior",
                "Complete 5 consecutive days of workouts",
                BadgeType.STREAK,
                BadgeCriteriaType.WORKOUT_STREAK,
                Map.of("targetValue", 5),
                100
        );

        createBadge(
                "STREAK_7",
                "Week Champion",
                "Complete 7 consecutive days of workouts",
                BadgeType.STREAK,
                BadgeCriteriaType.WORKOUT_STREAK,
                Map.of("targetValue", 7),
                150
        );

        createBadge(
                "STREAK_14",
                "Two Week Warrior",
                "Complete 14 consecutive days of workouts",
                BadgeType.STREAK,
                BadgeCriteriaType.WORKOUT_STREAK,
                Map.of("targetValue", 14),
                300
        );

        createBadge(
                "STREAK_30",
                "Monthly Master",
                "Complete 30 consecutive days of workouts",
                BadgeType.STREAK,
                BadgeCriteriaType.WORKOUT_STREAK,
                Map.of("targetValue", 30),
                500
        );

        createBadge(
                "STREAK_90",
                "Unstoppable Force",
                "Complete 90 consecutive days of workouts",
                BadgeType.CHAMPION,
                BadgeCriteriaType.WORKOUT_STREAK,
                Map.of("targetValue", 90),
                1500
        );

        createBadge(
                "STREAK_365",
                "Year-Long Legend",
                "Complete 365 consecutive days of workouts",
                BadgeType.CHAMPION,
                BadgeCriteriaType.WORKOUT_STREAK,
                Map.of("targetValue", 365),
                5000
        );
    }

    // ===========================================
    // PERSONAL RECORD BADGES
    // ===========================================

    private void createPRBadges() {
        log.info("Creating personal record badges...");

        createBadge(
                "FIRST_PR",
                "Record Breaker",
                "Achieved your first personal record!",
                BadgeType.PR,
                BadgeCriteriaType.PERSONAL_RECORD,
                Map.of("targetValue", 1),
                50
        );

        createBadge(
                "PR_5",
                "PR Hunter",
                "Achieved 5 personal records",
                BadgeType.PR,
                BadgeCriteriaType.PERSONAL_RECORD,
                Map.of("targetValue", 5),
                150
        );

        createBadge(
                "PR_10",
                "PR Master",
                "Achieved 10 personal records",
                BadgeType.PR,
                BadgeCriteriaType.PERSONAL_RECORD,
                Map.of("targetValue", 10),
                300
        );

        createBadge(
                "PR_25",
                "PR Dominator",
                "Achieved 25 personal records",
                BadgeType.CHAMPION,
                BadgeCriteriaType.PERSONAL_RECORD,
                Map.of("targetValue", 25),
                800
        );
    }

    // ===========================================
    // LEVEL ACHIEVEMENT BADGES
    // ===========================================

    private void createLevelBadges() {
        log.info("Creating level badges...");

        createBadge(
                "LEVEL_5",
                "Rising Star",
                "Reached Level 5!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.LEVEL_REACHED,
                Map.of("targetValue", 5),
                50
        );

        createBadge(
                "LEVEL_10",
                "Experienced Athlete",
                "Reached Level 10!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.LEVEL_REACHED,
                Map.of("targetValue", 10),
                100
        );

        createBadge(
                "LEVEL_20",
                "Fitness Elite",
                "Reached Level 20!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.LEVEL_REACHED,
                Map.of("targetValue", 20),
                250
        );

        createBadge(
                "LEVEL_50",
                "Legendary",
                "Reached Level 50!",
                BadgeType.CHAMPION,
                BadgeCriteriaType.LEVEL_REACHED,
                Map.of("targetValue", 50),
                1000
        );
    }

    // ===========================================
    // WORKOUT DURATION BADGES
    // ===========================================

    private void createWorkoutDurationBadges() {
        log.info("Creating workout duration badges...");

        createBadge(
                "HOUR_WORKOUT",
                "Hour Warrior",
                "Worked out for over an hour!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_DURATION,
                Map.of("targetValue", 60),
                30
        );

        createBadge(
                "MARATHON_SESSION",
                "Marathon Session",
                "Completed a 2-hour workout!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WORKOUT_DURATION,
                Map.of("targetValue", 120),
                100
        );
    }

    // ===========================================
    // WEIGHT LIFTED BADGES
    // ===========================================

    private void createWeightLiftedBadges() {
        log.info("Creating weight lifted badges...");

        createBadge(
                "WEIGHT_1000KG",
                "Ton Lifter",
                "Lifted a total of 1,000 kg!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WEIGHT_LIFTED_TOTAL,
                Map.of("targetValue", 1000.0),
                200
        );

        createBadge(
                "WEIGHT_5000KG",
                "Mass Mover",
                "Lifted a total of 5,000 kg!",
                BadgeType.EXERCISE,
                BadgeCriteriaType.WEIGHT_LIFTED_TOTAL,
                Map.of("targetValue", 5000.0),
                500
        );

        createBadge(
                "WEIGHT_10000KG",
                "Iron Champion",
                "Lifted a total of 10,000 kg!",
                BadgeType.CHAMPION,
                BadgeCriteriaType.WEIGHT_LIFTED_TOTAL,
                Map.of("targetValue", 10000.0),
                1000
        );
    }

    // ===========================================
    // EXERCISE VARIETY BADGES
    // ===========================================

    private void createExerciseVarietyBadges() {
        log.info("Creating exercise variety badges...");

        createBadge(
                "EXERCISE_10",
                "Variety Seeker",
                "Performed 10 different exercises",
                BadgeType.EXERCISE,
                BadgeCriteriaType.EXERCISE_COUNT,
                Map.of("targetValue", 10),
                50
        );

        createBadge(
                "EXERCISE_25",
                "Movement Master",
                "Performed 25 different exercises",
                BadgeType.EXERCISE,
                BadgeCriteriaType.EXERCISE_COUNT,
                Map.of("targetValue", 25),
                150
        );

        createBadge(
                "EXERCISE_50",
                "Exercise Encyclopedia",
                "Performed 50 different exercises",
                BadgeType.CHAMPION,
                BadgeCriteriaType.EXERCISE_COUNT,
                Map.of("targetValue", 50),
                400
        );
    }

    // ===========================================
    // CONSISTENCY BADGES
    // ===========================================

    private void createConsistencyBadges() {
        log.info("Creating consistency badges...");

        createBadge(
                "WEEKLY_WARRIOR",
                "Weekly Warrior",
                "Completed 5 workouts in a single week",
                BadgeType.STREAK,
                BadgeCriteriaType.WEEKLY_WORKOUTS,
                Map.of("targetValue", 5),
                100
        );

        createBadge(
                "MONTHLY_CHAMPION",
                "Monthly Champion",
                "Completed 20 workouts in a single month",
                BadgeType.STREAK,
                BadgeCriteriaType.MONTHLY_WORKOUTS,
                Map.of("targetValue", 20),
                300
        );

        createBadge(
                "LOGIN_STREAK_7",
                "Daily Devotee",
                "Logged in for 7 consecutive days",
                BadgeType.STREAK,
                BadgeCriteriaType.LOGIN_STREAK,
                Map.of("targetValue", 7),
                50
        );

        createBadge(
                "LOGIN_STREAK_30",
                "Ever-Present",
                "Logged in for 30 consecutive days",
                BadgeType.STREAK,
                BadgeCriteriaType.LOGIN_STREAK,
                Map.of("targetValue", 30),
                200
        );
    }

    // ===========================================
    // CHAMPION BADGES (Elite Achievements)
    // ===========================================

    private void createChampionBadges() {
        log.info("Creating champion badges...");

        createBadge(
                "VOLUME_CRUSHER",
                "Volume Crusher",
                "Completed 20+ sets in a single workout!",
                BadgeType.CHAMPION,
                BadgeCriteriaType.WORKOUT_COUNT,
                Map.of("targetValue", 20),
                200
        );

        createBadge(
                "POINTS_5000",
                "Point Master",
                "Earned 5,000 total points",
                BadgeType.CHAMPION,
                BadgeCriteriaType.POINTS_EARNED,
                Map.of("targetValue", 5000),
                500
        );

        createBadge(
                "POINTS_10000",
                "Point Legend",
                "Earned 10,000 total points",
                BadgeType.CHAMPION,
                BadgeCriteriaType.POINTS_EARNED,
                Map.of("targetValue", 10000),
                1000
        );
    }

    // ===========================================
    // HELPER METHOD
    // ===========================================

    private void createBadge(
            String badgeId,
            String name,
            String description,
            BadgeType badgeType,
            BadgeCriteriaType criteriaType,
            Map<String, Object> criteriaParams,
            int pointsAwarded) {

        Badge badge = new Badge();
        badge.setBadgeId(badgeId);
        badge.setName(name);
        badge.setDescription(description);
        badge.setBadgeType(badgeType);
        badge.setCriteriaType(criteriaType);
        badge.setCriteriaParams(new HashMap<>(criteriaParams));
        badge.setPointsAwarded(pointsAwarded);
        badge.setCreatedAt(Instant.now());

        badgeRepository.save(badge);
        log.debug("Created badge: {} ({})", name, badgeId);
    }
}
