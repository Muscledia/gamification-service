package com.muscledia.Gamification_service.testdata;

import com.muscledia.Gamification_service.model.*;
import com.muscledia.Gamification_service.model.enums.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Comprehensive test data builder for creating consistent test entities.
 * Provides fluent API for building test data with sensible defaults.
 */
public class TestDataBuilder {

    // ========== Badge Builder ==========
    public static class BadgeBuilder {
        private String badgeId = "test-badge-" + UUID.randomUUID().toString().substring(0, 8);
        private String name = "Test Badge";
        private String description = "A test badge for unit testing";
        private BadgeType badgeType = BadgeType.EXERCISE;
        private String imageUrl = "https://example.com/badge.png";
        private int pointsAwarded = 100;
        private BadgeCriteriaType criteriaType = BadgeCriteriaType.WORKOUT_COUNT;
        private Map<String, Object> criteriaParams = Map.of("count", 10);
        private Instant createdAt = Instant.now();

        public BadgeBuilder withId(String badgeId) {
            this.badgeId = badgeId;
            return this;
        }

        public BadgeBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public BadgeBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public BadgeBuilder withType(BadgeType badgeType) {
            this.badgeType = badgeType;
            return this;
        }

        public BadgeBuilder withImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public BadgeBuilder withPoints(int points) {
            this.pointsAwarded = points;
            return this;
        }

        public BadgeBuilder withCriteriaType(BadgeCriteriaType criteriaType) {
            this.criteriaType = criteriaType;
            return this;
        }

        public BadgeBuilder withCriteriaParams(Map<String, Object> params) {
            this.criteriaParams = params;
            return this;
        }

        public BadgeBuilder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Badge build() {
            Badge badge = new Badge();
            badge.setBadgeId(badgeId);
            badge.setName(name);
            badge.setDescription(description);
            badge.setBadgeType(badgeType);
            badge.setImageUrl(imageUrl);
            badge.setPointsAwarded(pointsAwarded);
            badge.setCriteriaType(criteriaType);
            badge.setCriteriaParams(criteriaParams);
            badge.setCreatedAt(createdAt);
            return badge;
        }
    }

    // ========== Quest Builder ==========
    public static class QuestBuilder {
        private String id = "test-quest-" + UUID.randomUUID().toString().substring(0, 8);
        private String name = "Test Quest";
        private String description = "A test quest for unit testing";
        private QuestType questType = QuestType.DAILY;
        private ObjectiveType objectiveType = ObjectiveType.EXERCISES;
        private int objectiveTarget = 5;
        private int expReward = 50;
        private int pointsReward = 100;
        private Instant startDate = Instant.now().minus(1, ChronoUnit.HOURS);
        private Instant endDate = Instant.now().plus(23, ChronoUnit.HOURS);
        private boolean repeatable = false;
        private String exerciseId = null;
        private String muscleGroupId = null;
        private int requiredLevel = 1;
        private Instant createdAt = Instant.now();

        public QuestBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public QuestBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public QuestBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public QuestBuilder withType(QuestType questType) {
            this.questType = questType;
            return this;
        }

        public QuestBuilder withObjectiveType(ObjectiveType objectiveType) {
            this.objectiveType = objectiveType;
            return this;
        }

        public QuestBuilder withObjectiveTarget(int target) {
            this.objectiveTarget = target;
            return this;
        }

        public QuestBuilder withExpReward(int exp) {
            this.expReward = exp;
            return this;
        }

        public QuestBuilder withPointsReward(int points) {
            this.pointsReward = points;
            return this;
        }

        public QuestBuilder withStartDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public QuestBuilder withEndDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        public QuestBuilder withRepeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        public QuestBuilder withExerciseId(String exerciseId) {
            this.exerciseId = exerciseId;
            return this;
        }

        public QuestBuilder withMuscleGroupId(String muscleGroupId) {
            this.muscleGroupId = muscleGroupId;
            return this;
        }

        public QuestBuilder withRequiredLevel(int level) {
            this.requiredLevel = level;
            return this;
        }

        public QuestBuilder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Quest build() {
            Quest quest = new Quest();
            quest.setId(id);
            quest.setName(name);
            quest.setDescription(description);
            quest.setQuestType(questType);
            quest.setObjectiveType(objectiveType);
            quest.setObjectiveTarget(objectiveTarget);
            quest.setExpReward(expReward);
            quest.setPointsReward(pointsReward);
            quest.setStartDate(startDate);
            quest.setEndDate(endDate);
            quest.setRepeatable(repeatable);
            quest.setExerciseId(exerciseId);
            quest.setMuscleGroupId(muscleGroupId);
            quest.setRequiredLevel(requiredLevel);
            quest.setCreatedAt(createdAt);
            return quest;
        }
    }

    // ========== Champion Builder ==========
    public static class ChampionBuilder {
        private String id = "test-champion-" + UUID.randomUUID().toString().substring(0, 8);
        private String name = "Test Champion";
        private String description = "A test champion for unit testing";
        private String imageUrl = "https://example.com/champion.png";
        private String requiredExerciseId = "bench-press";
        private int baseDifficulty = 5;
        private String muscleGroupId = "chest";
        private ChampionCriteriaType criteriaType = ChampionCriteriaType.PERSONAL_RECORD_WEIGHT;
        private Map<String, Object> criteriaParams = Map.of("weight", 225.0, "reps", 1);
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();

        public ChampionBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public ChampionBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ChampionBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public ChampionBuilder withImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public ChampionBuilder withRequiredExerciseId(String exerciseId) {
            this.requiredExerciseId = exerciseId;
            return this;
        }

        public ChampionBuilder withBaseDifficulty(int difficulty) {
            this.baseDifficulty = difficulty;
            return this;
        }

        public ChampionBuilder withMuscleGroupId(String muscleGroupId) {
            this.muscleGroupId = muscleGroupId;
            return this;
        }

        public ChampionBuilder withCriteriaType(ChampionCriteriaType criteriaType) {
            this.criteriaType = criteriaType;
            return this;
        }

        public ChampionBuilder withCriteriaParams(Map<String, Object> params) {
            this.criteriaParams = params;
            return this;
        }

        public ChampionBuilder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ChampionBuilder withUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Champion build() {
            Champion champion = new Champion();
            champion.setId(id);
            champion.setName(name);
            champion.setDescription(description);
            champion.setImageUrl(imageUrl);
            champion.setRequiredExerciseId(requiredExerciseId);
            champion.setBaseDifficulty(baseDifficulty);
            champion.setMuscleGroupId(muscleGroupId);
            champion.setCriteriaType(criteriaType);
            champion.setCriteriaParams(criteriaParams);
            champion.setCreatedAt(createdAt);
            champion.setUpdatedAt(updatedAt);
            return champion;
        }
    }

    // ========== UserGamificationProfile Builder ==========
    public static class UserProfileBuilder {
        private Long userId = 12345L;
        private int points = 500;
        private int level = 3;
        private Instant lastLevelUpDate = Instant.now().minus(2, ChronoUnit.DAYS);
        private Map<String, UserGamificationProfile.StreakData> streaks = new HashMap<>();
        private List<UserBadge> earnedBadges = new ArrayList<>();
        private List<UserQuestProgress> quests = new ArrayList<>();
        private Integer weeklyStreak = 0;
        private Integer longestWeeklyStreak = 0;
        private Integer monthlyStreak = 0;
        private Integer longestMonthlyStreak = 0;
        private Integer restDaysSinceLastWorkout = 0;
        private Integer totalWorkoutsCompleted = 0;

        public UserProfileBuilder() {
            // Default streak data
            UserGamificationProfile.StreakData workoutStreak = new UserGamificationProfile.StreakData();
            workoutStreak.setCurrent(7);
            workoutStreak.setLastUpdate(Instant.now());
            workoutStreak.setLongest(14);
            streaks.put("workout", workoutStreak);
        }

        public UserProfileBuilder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserProfileBuilder withPoints(int points) {
            this.points = points;
            return this;
        }

        public UserProfileBuilder withLevel(int level) {
            this.level = level;
            return this;
        }

        public UserProfileBuilder withLastLevelUpDate(Instant date) {
            this.lastLevelUpDate = date;
            return this;
        }

        public UserProfileBuilder withStreaks(Map<String, UserGamificationProfile.StreakData> streaks) {
            this.streaks = streaks;
            return this;
        }

        public UserProfileBuilder withEarnedBadges(List<UserBadge> badges) {
            this.earnedBadges = badges;
            return this;
        }

        public UserProfileBuilder withQuests(List<UserQuestProgress> quests) {
            this.quests = quests;
            return this;
        }

        public UserProfileBuilder withWeeklyStreak(Integer weeklyStreak) {
            this.weeklyStreak = weeklyStreak;
            return this;
        }

        public UserProfileBuilder withLongestWeeklyStreak(Integer longestWeeklyStreak) {
            this.longestWeeklyStreak = longestWeeklyStreak;
            return this;
        }

        public UserProfileBuilder withMonthlyStreak(Integer monthlyStreak) {
            this.monthlyStreak = monthlyStreak;
            return this;
        }

        public UserProfileBuilder withLongestMonthlyStreak(Integer longestMonthlyStreak) {
            this.longestMonthlyStreak = longestMonthlyStreak;
            return this;
        }

        public UserProfileBuilder withRestDaysSinceLastWorkout(Integer restDays) {
            this.restDaysSinceLastWorkout = restDays;
            return this;
        }

        public UserProfileBuilder withTotalWorkoutsCompleted(Integer totalWorkouts) {
            this.totalWorkoutsCompleted = totalWorkouts;
            return this;
        }

        public UserProfileBuilder addBadge(UserBadge badge) {
            this.earnedBadges.add(badge);
            return this;
        }

        public UserProfileBuilder addQuest(UserQuestProgress quest) {
            this.quests.add(quest);
            return this;
        }

        public UserGamificationProfile build() {
            return UserGamificationProfile.builder()
                    .userId(userId)
                    .points(points)
                    .level(level)
                    .lastLevelUpDate(lastLevelUpDate)
                    .streaks(streaks)
                    .earnedBadges(earnedBadges)
                    .quests(quests)
                    .weeklyStreak(weeklyStreak)
                    .longestWeeklyStreak(longestWeeklyStreak)
                    .monthlyStreak(monthlyStreak)
                    .longestMonthlyStreak(longestMonthlyStreak)
                    .restDaysSinceLastWorkout(restDaysSinceLastWorkout)
                    .totalWorkoutsCompleted(totalWorkoutsCompleted)
                    .profileCreatedAt(Instant.now())
                    .lastUpdated(Instant.now())
                    .build();
        }
    }

    // ========== UserBadge Builder ==========
    public static class UserBadgeBuilder {
        private String badgeId = "test-badge-123";
        private Instant earnedAt = Instant.now();

        public UserBadgeBuilder withBadgeId(String badgeId) {
            this.badgeId = badgeId;
            return this;
        }

        public UserBadgeBuilder withEarnedAt(Instant earnedAt) {
            this.earnedAt = earnedAt;
            return this;
        }

        public UserBadge build() {
            UserBadge userBadge = new UserBadge();
            userBadge.setBadgeId(badgeId);
            userBadge.setEarnedAt(earnedAt);
            return userBadge;
        }
    }

    // ========== UserQuestProgress Builder ==========
    public static class UserQuestProgressBuilder {
        private String questId = "test-quest-123";
        private QuestStatus status = QuestStatus.IN_PROGRESS;
        private int objectiveProgress = 3;
        private Instant startDate = Instant.now().minus(2, ChronoUnit.HOURS);
        private Instant completionDate = null;

        public UserQuestProgressBuilder withQuestId(String questId) {
            this.questId = questId;
            return this;
        }

        public UserQuestProgressBuilder withStatus(QuestStatus status) {
            this.status = status;
            return this;
        }

        public UserQuestProgressBuilder withObjectiveProgress(int progress) {
            this.objectiveProgress = progress;
            return this;
        }

        public UserQuestProgressBuilder withStartDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public UserQuestProgressBuilder withCompletionDate(Instant completionDate) {
            this.completionDate = completionDate;
            return this;
        }

        public UserQuestProgress build() {
            UserQuestProgress progress = new UserQuestProgress();
            progress.setQuestId(questId);
            progress.setStatus(status);
            progress.setObjectiveProgress(objectiveProgress);
            progress.setStartDate(startDate);
            progress.setCompletionDate(completionDate);
            progress.setCreatedAt(Instant.now());
            return progress;
        }
    }

    // ========== Factory Methods ==========
    public static BadgeBuilder badge() {
        return new BadgeBuilder();
    }

    public static QuestBuilder quest() {
        return new QuestBuilder();
    }

    public static ChampionBuilder champion() {
        return new ChampionBuilder();
    }

    public static UserProfileBuilder userProfile() {
        return new UserProfileBuilder();
    }

    public static UserBadgeBuilder userBadge() {
        return new UserBadgeBuilder();
    }

    public static UserQuestProgressBuilder userQuestProgress() {
        return new UserQuestProgressBuilder();
    }

    // ========== Common Test Scenarios ==========

    public static Badge createStreakBadge() {
        return badge()
                .withName("Streak Master")
                .withType(BadgeType.STREAK)
                .withCriteriaType(BadgeCriteriaType.WORKOUT_STREAK)
                .withCriteriaParams(Map.of("streakType", "workout", "days", 7))
                .withPoints(200)
                .build();
    }

    public static Quest createDailyWorkoutQuest() {
        return quest()
                .withName("Daily Warrior")
                .withType(QuestType.DAILY)
                .withObjectiveType(ObjectiveType.EXERCISES)
                .withObjectiveTarget(1)
                .withExpReward(25)
                .withPointsReward(50)
                .build();
    }

    public static Champion createBenchPressChampion() {
        return champion()
                .withName("Bench Press Beast")
                .withRequiredExerciseId("bench-press")
                .withBaseDifficulty(8)
                .withMuscleGroupId("chest")
                .withCriteriaType(ChampionCriteriaType.PERSONAL_RECORD_WEIGHT)
                .withCriteriaParams(Map.of("weight", 315.0, "reps", 1))
                .build();
    }

    public static UserGamificationProfile createActiveUser() {
        return userProfile()
                .withUserId(12345L)
                .withPoints(1500)
                .withLevel(5)
                .addBadge(userBadge().withBadgeId("first-workout").build())
                .addQuest(userQuestProgress().withStatus(QuestStatus.IN_PROGRESS).withObjectiveProgress(2).build())
                .build();
    }
}