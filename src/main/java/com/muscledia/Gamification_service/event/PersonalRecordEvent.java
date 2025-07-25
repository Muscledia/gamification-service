package com.muscledia.Gamification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Event triggered when a user achieves a new personal record.
 * This is an INBOUND event that triggers high-value badge evaluations.
 * 
 * Senior Engineering Note: PR events are significant achievements and typically
 * award substantial points and may unlock exclusive badges.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PersonalRecordEvent extends BaseEvent {

    /**
     * Exercise for which the PR was achieved
     */
    @NotBlank
    private String exerciseName;

    /**
     * Type of personal record (e.g., "1RM", "volume", "duration")
     */
    @NotBlank
    private String recordType;

    /**
     * New record value
     */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private Double newValue;

    /**
     * Previous record value (for improvement calculation)
     */
    private Double previousValue;

    /**
     * Unit of measurement (e.g., "lbs", "kg", "minutes", "reps")
     */
    @NotBlank
    private String unit;

    /**
     * Workout ID where this PR was achieved
     */
    @NotBlank
    private String workoutId;

    /**
     * Set number where PR was achieved
     */
    private Integer setNumber;

    /**
     * Number of reps performed (for weight-based PRs)
     */
    private Integer reps;

    /**
     * When the PR was achieved
     */
    @NotNull
    private Instant achievedAt;

    @Override
    public String getEventType() {
        return "PERSONAL_RECORD";
    }

    @Override
    public boolean isValid() {
        return exerciseName != null && !exerciseName.trim().isEmpty()
                && recordType != null && !recordType.trim().isEmpty()
                && newValue != null && newValue > 0
                && unit != null && !unit.trim().isEmpty()
                && workoutId != null && !workoutId.trim().isEmpty()
                && achievedAt != null;
    }

    /**
     * Calculate improvement percentage over previous record
     */
    public double getImprovementPercentage() {
        if (previousValue == null || previousValue <= 0) {
            return 100.0; // First PR is 100% improvement
        }

        return ((newValue - previousValue) / previousValue) * 100.0;
    }

    /**
     * Determine if this PR qualifies for milestone badges
     */
    public boolean isMilestonePR() {
        return switch (unit.toLowerCase()) {
            case "lbs", "kg" -> isWeightMilestone();
            case "minutes", "seconds" -> isDurationMilestone();
            default -> false;
        };
    }

    private boolean isWeightMilestone() {
        // Check for common weight milestones
        if ("lbs".equalsIgnoreCase(unit)) {
            return newValue >= 135 && newValue % 45 == 0; // 135, 180, 225, etc.
        } else if ("kg".equalsIgnoreCase(unit)) {
            return newValue >= 60 && newValue % 20 == 0; // 60, 80, 100, etc.
        }
        return false;
    }

    private boolean isDurationMilestone() {
        // Check for time-based milestones (in minutes)
        return newValue >= 30 && newValue % 15 == 0; // 30, 45, 60 minutes, etc.
    }

    /**
     * Get the significance level of this PR for badge awarding
     */
    public String getSignificanceLevel() {
        double improvement = getImprovementPercentage();

        if (improvement >= 50)
            return "MAJOR";
        if (improvement >= 25)
            return "SIGNIFICANT";
        if (improvement >= 10)
            return "NOTABLE";
        return "MINOR";
    }
}