package com.muscledia.Gamification_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_gamification_profiles")
public class UserGamificationProfile {
    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    // === XP SYSTEM (Cannot be spent) ===
    @Builder.Default
    private Integer points = 0;  // XP for leveling

    @Builder.Default
    private Integer level = 1;

    private Instant lastLevelUpDate;

    // === REWARD CURRENCY (Can be spent) === ⬅️ NEW
    @Builder.Default
    private Integer fitnessCoins = 0;

    @Builder.Default
    private Integer lifetimeCoinsEarned = 0;

    // === EXISTING FIELDS ===
    private String username;

    @Builder.Default
    private Map<String, StreakData> streaks = new HashMap<>();

    @Builder.Default
    private List<UserBadge> earnedBadges = new ArrayList<>();

    @Builder.Default
    private List<UserQuestProgress> quests = new ArrayList<>();

    @Builder.Default
    private Integer totalWorkoutsCompleted = 0;

    private Instant lastWorkoutDate;

    // Weekly Streak
    @Builder.Default
    private Integer weeklyStreak = 0;

    @Builder.Default
    private Integer longestWeeklyStreak = 0;

    private Instant currentWeekStartDate;

    // Monthly Streak
    @Builder.Default
    private Integer monthlyStreak = 0;

    @Builder.Default
    private Integer longestMonthlyStreak = 0;

    private Instant currentMonthStartDate;

    @Builder.Default
    private Integer restDaysSinceLastWorkout = 0;

    // === NEW FIELDS FOR ENHANCED GAMIFICATION === ⬅️ ADD THESE

    // Personal Records tracking
    @Builder.Default
    private Integer totalPersonalRecords = 0;

    // Total workout time (for badge criteria)
    @Builder.Default
    private Integer totalWorkoutMinutes = 0;

    // Store inventory
    @Builder.Default
    private List<OwnedItem> inventory = new ArrayList<>();

    // Active challenges
    @Builder.Default
    private List<ActiveChallenge> activeChallenges = new ArrayList<>();

    // Leaderboard stats cache (updated periodically)
    private LeaderboardStats leaderboardStats;

    // Timestamps
    private Instant profileCreatedAt;
    private Instant lastUpdated;


    // === NEW NESTED CLASSES === ⬅️ ADD THESE

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnedItem {
        private String itemId;
        private LocalDateTime purchasedAt;
        private Boolean isActive;  // For equipped items (avatars, themes)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveChallenge {
        private String challengeId;
        private Integer progress;
        private Integer target;
        private LocalDateTime startedAt;
        private LocalDateTime expiresAt;
        private String status;  // IN_PROGRESS, COMPLETED, FAILED, CLAIMED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardStats {
        private Integer weeklyWorkouts;
        private Integer monthlyWorkouts;
        private Integer weeklyVolume;  // kg
        private Integer monthlyPersonalRecords;
        private LocalDateTime lastUpdated;
    }

    // === EXISTING NESTED CLASS ===
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StreakData {
        @Builder.Default
        private Integer current = 0;
        private Instant lastUpdate;
        @Builder.Default
        private Integer longest = 0;
    }

    /**
     * Add points and handle level-up logic
     */
    public void addPoints(int pointsToAdd) {
        if (this.points == null) {
            this.points = 0;
        }

        this.points += pointsToAdd;
        this.lastUpdated = Instant.now();

        // Check for level up
        updateLevel();
    }

    /**
     * Calculate and update level based on points
     * Level formula: level = floor(sqrt(points / 100)) + 1
     * Level 1: 0-99 points
     * Level 2: 100-399 points
     * Level 3: 400-899 points
     * etc.
     */
    private void updateLevel() {
        if (this.points == null) {
            this.points = 0;
        }

        int newLevel = (int) Math.floor(Math.sqrt(this.points / 100.0)) + 1;

        if (newLevel > this.level) {
            this.level = newLevel;
            this.lastLevelUpDate = Instant.now();
        }
    }

    /**
     * Add fitness coins (reward currency)
     */
    public void addFitnessCoins(int coinsToAdd) {
        if (this.fitnessCoins == null) {
            this.fitnessCoins = 0;
        }
        if (this.lifetimeCoinsEarned == null) {
            this.lifetimeCoinsEarned = 0;
        }

        this.fitnessCoins += coinsToAdd;
        this.lifetimeCoinsEarned += coinsToAdd;
        this.lastUpdated = Instant.now();
    }

    /**
     * Spend fitness coins (for store purchases)
     */
    public boolean spendFitnessCoins(int coinsToSpend) {
        if (this.fitnessCoins == null || this.fitnessCoins < coinsToSpend) {
            return false; // Not enough coins
        }

        this.fitnessCoins -= coinsToSpend;
        this.lastUpdated = Instant.now();
        return true;
    }

    // === EXISTING METHODS ===
    public void addBadge(UserBadge badge) {
        if (this.earnedBadges == null) {
            this.earnedBadges = new ArrayList<>();
        }

        boolean alreadyHas = this.earnedBadges.stream()
                .anyMatch(existingBadge -> badge.getBadgeId().equals(existingBadge.getBadgeId()));

        if (!alreadyHas) {
            this.earnedBadges.add(badge);
            this.lastUpdated = Instant.now();
        }
    }

    public boolean hasBadge(String badgeId) {
        if (earnedBadges == null) return false;
        return earnedBadges.stream()
                .anyMatch(badge -> badgeId.equals(badge.getBadgeId()));
    }

    public void incrementWorkoutCount() {
        if (this.totalWorkoutsCompleted == null) {
            this.totalWorkoutsCompleted = 0;
        }
        this.totalWorkoutsCompleted++;
        this.lastWorkoutDate = Instant.now();
        this.lastUpdated = Instant.now();
    }

    public void initializeDefaults() {
        if (this.earnedBadges == null) {
            this.earnedBadges = new ArrayList<>();
        }
        if (this.streaks == null) {
            this.streaks = new HashMap<>();
        }
        if (this.quests == null) {
            this.quests = new ArrayList<>();
        }
        if (this.points == null) {
            this.points = 0;
        }
        if (this.level == null) {
            this.level = 1;
        }
        if (this.totalWorkoutsCompleted == null) {
            this.totalWorkoutsCompleted = 0;
        }
        if (this.weeklyStreak == null) {
            this.weeklyStreak = 0;
        }
        if (this.longestWeeklyStreak == null) {
            this.longestWeeklyStreak = 0;
        }
        if (this.monthlyStreak == null) {
            this.monthlyStreak = 0;
        }
        if (this.longestMonthlyStreak == null) {
            this.longestMonthlyStreak = 0;
        }
        if (this.restDaysSinceLastWorkout == null) {
            this.restDaysSinceLastWorkout = 0;
        }

        // === NEW DEFAULTS === ⬅️ ADD THESE
        if (this.fitnessCoins == null) {
            this.fitnessCoins = 0;
        }
        if (this.lifetimeCoinsEarned == null) {
            this.lifetimeCoinsEarned = 0;
        }
        if (this.totalPersonalRecords == null) {
            this.totalPersonalRecords = 0;
        }
        if (this.totalWorkoutMinutes == null) {
            this.totalWorkoutMinutes = 0;
        }
        if (this.inventory == null) {
            this.inventory = new ArrayList<>();
        }
        if (this.activeChallenges == null) {
            this.activeChallenges = new ArrayList<>();
        }
    }
}