package com.muscledia.Gamification_service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Event published when a user levels up.
 * This is an OUTBOUND event that triggers celebrations and unlock
 * notifications.
 * 
 * Senior Engineering Note: Level ups are major milestones that may unlock
 * new quests, badges, or features in other services.
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class LevelUpEvent extends BaseEvent {

    /**
     * User's previous level
     */
    @NotNull
    @Min(1)
    private Integer previousLevel;

    /**
     * User's new level
     */
    @NotNull
    @Min(1)
    private Integer newLevel;

    /**
     * Total points the user has
     */
    @NotNull
    @Min(0)
    private Integer totalPoints;

    /**
     * Points required for the next level
     */
    @Min(0)
    private Integer pointsToNextLevel;

    /**
     * When the level up occurred
     */
    @NotNull
    private Instant levelUpAt;

    /**
     * Activity that triggered the level up
     */
    private String triggeringActivity;

    /**
     * Reference to the event that caused the level up
     */
    private String triggeringEventId;

    /**
     * New features or content unlocked at this level
     */
    private Map<String, Object> unlockedFeatures;

    /**
     * Time since last level up
     */
    private Long daysSinceLastLevelUp;

    /**
     * Default constructor for Jackson and Lombok
     */
    public LevelUpEvent() {
        super();
    }

    @Override
    public String getEventType() {
        return "LEVEL_UP";
    }

    @Override
    public boolean isValid() {
        return previousLevel != null && previousLevel >= 1
                && newLevel != null && newLevel >= 1
                && newLevel > previousLevel
                && totalPoints != null && totalPoints >= 0
                && levelUpAt != null;
    }

    @Override
    public BaseEvent withNewTimestamp() {
        return this.toBuilder()
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Calculate the number of levels gained
     */
    public int getLevelsGained() {
        return newLevel - previousLevel;
    }

    /**
     * Check if this is a significant level milestone
     */
    public boolean isMilestoneLevel() {
        return newLevel % 10 == 0 || // Every 10th level
                newLevel == 25 || newLevel == 50 || newLevel == 75 || // Special milestones
                newLevel == 100; // Century mark
    }

    /**
     * Get the significance of this level up
     */
    public String getLevelUpSignificance() {
        if (newLevel >= 100)
            return "LEGENDARY";
        if (newLevel >= 50)
            return "EPIC";
        if (isMilestoneLevel())
            return "MILESTONE";
        if (getLevelsGained() > 1)
            return "MULTI_LEVEL";
        return "STANDARD";
    }

    /**
     * Check if this level up was rapid (fast progression)
     */
    public boolean isRapidProgression() {
        return daysSinceLastLevelUp != null && daysSinceLastLevelUp <= 1;
    }

    /**
     * Create a congratulatory message for the level up
     */
    public String getCongratulationMessage() {
        StringBuilder message = new StringBuilder();
        message.append("🌟 Level Up! You've reached Level ").append(newLevel);

        if (getLevelsGained() > 1) {
            message.append(" (jumped ").append(getLevelsGained()).append(" levels!)");
        }

        if (isMilestoneLevel()) {
            message.append(" - Milestone Achievement!");
        }

        if (unlockedFeatures != null && !unlockedFeatures.isEmpty()) {
            message.append(" New features unlocked!");
        }

        return message.toString();
    }

    /**
     * Get estimated time to next level based on current progression
     */
    public String getNextLevelEstimate() {
        if (pointsToNextLevel == null || daysSinceLastLevelUp == null || daysSinceLastLevelUp == 0) {
            return "Unknown";
        }

        // Simple estimation based on recent progression
        int pointsGainedForCurrentLevel = calculatePointsForLevel(newLevel) - calculatePointsForLevel(previousLevel);
        double pointsPerDay = (double) pointsGainedForCurrentLevel / daysSinceLastLevelUp;

        if (pointsPerDay <= 0)
            return "Unknown";

        long estimatedDays = Math.round(pointsToNextLevel / pointsPerDay);

        if (estimatedDays <= 1)
            return "1 day";
        if (estimatedDays <= 7)
            return estimatedDays + " days";
        if (estimatedDays <= 30)
            return Math.round(estimatedDays / 7.0) + " weeks";
        return Math.round(estimatedDays / 30.0) + " months";
    }

    private int calculatePointsForLevel(int level) {
        // This should match the calculation in UserGamificationService
        if (level <= 1)
            return 0;
        if (level <= 2)
            return 100;
        if (level <= 3)
            return 300;
        if (level <= 4)
            return 600;
        if (level <= 5)
            return 1000;
        if (level <= 6)
            return 1500;
        if (level <= 7)
            return 2100;
        if (level <= 8)
            return 2800;
        if (level <= 9)
            return 3600;
        if (level <= 10)
            return 4500;
        return 4500 + (level - 10) * 1000;
    }
}