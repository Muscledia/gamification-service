package com.muscledia.Gamification_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Data
@Document(collection = "user_gamification_profiles")
public class UserGamificationProfile {
    @Id
    private String id;

    @Indexed(unique = true)
    private Long userId;

    @Builder.Default
    private Integer points = 0;

    private String username;

    @Builder.Default
    private Integer level = 1;

    private Instant lastLevelUpDate;

    @Builder.Default
    private Map<String, StreakData> streaks = new HashMap<>();

    @Builder.Default
    private List<UserBadge> earnedBadges = new ArrayList<>();

    @Builder.Default
    private List<UserQuestProgress> quests = new ArrayList<>();

    @Builder.Default
    private Integer totalWorkoutsCompleted = 0;

    private Instant lastWorkoutDate;

    // Weekly Streak Fields
    @Builder.Default
    private Integer weeklyStreak = 0;

    @Builder.Default
    private Integer longestWeeklyStreak = 0;

    private Instant currentWeekStartDate;

    // Monthly Streak Fields
    @Builder.Default
    private Integer monthlyStreak = 0;

    @Builder.Default
    private Integer longestMonthlyStreak = 0;

    private Instant currentMonthStartDate;

    // Shared Fields
    @Builder.Default
    private Integer restDaysSinceLastWorkout = 0;

    private Instant profileCreatedAt;
    private Instant lastUpdated;

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
    }
}