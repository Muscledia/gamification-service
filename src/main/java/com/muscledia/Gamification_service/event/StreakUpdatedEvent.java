package com.muscledia.Gamification_service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Event triggered when a user's streak is updated.
 * This can be both INBOUND (from other services) and OUTBOUND (internal streak
 * calculations).
 * 
 * Senior Engineering Note: Streak events trigger milestone badge evaluations
 * and can cascade into other gamification rewards.
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class StreakUpdatedEvent extends BaseEvent {

    /**
     * Type of streak (e.g., "workout", "login", "goal_completion")
     */
    @NotBlank
    private String streakType;

    /**
     * Current streak count
     */
    @NotNull
    @Min(0)
    private Integer currentStreak;

    /**
     * Previous streak count
     */
    @Min(0)
    private Integer previousStreak;

    /**
     * User's longest streak of this type
     */
    @NotNull
    @Min(0)
    private Integer longestStreak;

    /**
     * Whether the streak increased, decreased, or was reset
     */
    @NotBlank
    private String streakAction; // "INCREASED", "DECREASED", "RESET", "MAINTAINED"

    /**
     * Activity that triggered this streak update
     */
    private String triggeringActivity;

    /**
     * Reference to the event/activity that caused this update
     */
    private String triggeringEventId;

    /**
     * Default constructor for Jackson and Lombok
     */
    public StreakUpdatedEvent() {
        super();
    }

    @Override
    public String getEventType() {
        return "STREAK_UPDATED";
    }

    @Override
    public boolean isValid() {
        return streakType != null && !streakType.trim().isEmpty()
                && currentStreak != null && currentStreak >= 0
                && longestStreak != null && longestStreak >= 0
                && streakAction != null && !streakAction.trim().isEmpty()
                && isValidStreakAction();
    }

    @Override
    public BaseEvent withNewTimestamp() {
        return this.toBuilder()
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public double getIntensityScore() {
        return 0;
    }

    @Override
    public boolean isStreakEligible() {
        return false;
    }

    private boolean isValidStreakAction() {
        return switch (streakAction.toUpperCase()) {
            case "INCREASED", "DECREASED", "RESET", "MAINTAINED" -> true;
            default -> false;
        };
    }

    /**
     * Check if this streak update represents a milestone achievement
     */
    public boolean isMilestone() {
        return switch (streakType.toLowerCase()) {
            case "workout" -> isWorkoutMilestone();
            case "login" -> isLoginMilestone();
            default -> isGenericMilestone();
        };
    }

    private boolean isWorkoutMilestone() {
        // Workout streak milestones: 7, 14, 30, 60, 100 days
        return currentStreak != null && (currentStreak == 7 || currentStreak == 14 || currentStreak == 30 ||
                currentStreak == 60 || currentStreak == 100 ||
                (currentStreak > 100 && currentStreak % 50 == 0));
    }

    private boolean isLoginMilestone() {
        // Login streak milestones: 10, 30, 60, 100 days
        return currentStreak != null && (currentStreak == 10 || currentStreak == 30 || currentStreak == 60 ||
                currentStreak == 100 || (currentStreak > 100 && currentStreak % 100 == 0));
    }

    private boolean isGenericMilestone() {
        // Generic milestones for other streak types
        return currentStreak != null && (currentStreak == 5 || currentStreak == 10 || currentStreak == 25 ||
                currentStreak == 50 || (currentStreak > 50 && currentStreak % 25 == 0));
    }

    /**
     * Check if this is a new personal best streak
     */
    public boolean isNewRecord() {
        return currentStreak != null && longestStreak != null
                && currentStreak.equals(longestStreak)
                && "INCREASED".equals(streakAction);
    }

    /**
     * Get the magnitude of streak change
     */
    public int getStreakDelta() {
        if (previousStreak == null) {
            return currentStreak != null ? currentStreak : 0;
        }
        return (currentStreak != null ? currentStreak : 0) - previousStreak;
    }

    /**
     * Get the significance level of this streak update
     */
    public String getSignificanceLevel() {
        if (isNewRecord())
            return "RECORD";
        if (isMilestone())
            return "MILESTONE";
        if ("RESET".equals(streakAction) && previousStreak != null && previousStreak >= 7)
            return "SIGNIFICANT_LOSS";
        if ("INCREASED".equals(streakAction))
            return "PROGRESS";
        return "MINOR";
    }
}