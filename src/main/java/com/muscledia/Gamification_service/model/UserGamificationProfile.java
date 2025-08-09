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
    private String id;  // MongoDB requires String ID, not Long

    @Indexed(unique = true)
    private Long userId;

    @Builder.Default
    private Integer points = 0;

    @Builder.Default
    private Integer level = 1;

    private Instant lastLevelUpDate;

    @Builder.Default
    private Map<String, StreakData> streaks = new HashMap<>();

    @Builder.Default
    private List<UserBadge> earnedBadges = new ArrayList<>();

    @Builder.Default
    private List<UserQuestProgress> quests = new ArrayList<>();

    // ADDED: Workout tracking fields
    @Builder.Default
    private Integer totalWorkoutsCompleted = 0;

    private Instant lastWorkoutDate;
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

    /**
     * Add a badge to the user's collection
     */
    public void addBadge(UserBadge badge) {
        if (this.earnedBadges == null) {
            this.earnedBadges = new ArrayList<>();
        }

        // Check if badge already exists
        boolean exists = this.earnedBadges.stream()
                .anyMatch(existing -> existing.getBadgeId().equals(badge.getBadgeId()));

        if (!exists) {
            this.earnedBadges.add(badge);
            this.lastUpdated = Instant.now();
        }
    }

    /**
     * Check if user has a specific badge
     */
    public boolean hasBadge(String badgeId) {
        if (earnedBadges == null) return false;
        return earnedBadges.stream()
                .anyMatch(badge -> badgeId.equals(badge.getBadgeId()));
    }

    /**
     * Increment workout count
     */
    public void incrementWorkoutCount() {
        if (this.totalWorkoutsCompleted == null) {
            this.totalWorkoutsCompleted = 0;
        }
        this.totalWorkoutsCompleted++;
        this.lastWorkoutDate = Instant.now();
        this.lastUpdated = Instant.now();
    }

    /**
     * Initialize default values
     */
    public void initializeDefaults() {
        if (this.points == null) this.points = 0;
        if (this.level == null) this.level = 1;
        if (this.totalWorkoutsCompleted == null) this.totalWorkoutsCompleted = 0;
        if (this.streaks == null) this.streaks = new HashMap<>();
        if (this.earnedBadges == null) this.earnedBadges = new ArrayList<>();
        if (this.quests == null) this.quests = new ArrayList<>();
        if (this.profileCreatedAt == null) this.profileCreatedAt = Instant.now();
        this.lastUpdated = Instant.now();
    }
}
