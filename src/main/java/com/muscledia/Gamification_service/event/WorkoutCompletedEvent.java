package com.muscledia.Gamification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Event triggered when a user completes a workout.
 * This is an INBOUND event that triggers gamification processing.
 * 
 * Senior Engineering Note: This event comes from the workout service
 * and triggers real-time badge eligibility, quest progress, and streak
 * calculations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkoutCompletedEvent extends BaseEvent {

    /**
     * Unique identifier for the workout
     */
    @NotBlank
    private String workoutId;

    /**
     * Type of workout (e.g., "strength", "cardio", "hiit")
     */
    @NotBlank
    private String workoutType;

    /**
     * Duration of workout in minutes
     */
    @Min(1)
    private Integer durationMinutes;

    /**
     * Calories burned during workout
     */
    @Min(0)
    private Integer caloriesBurned;

    /**
     * Number of exercises completed
     */
    @Min(1)
    private Integer exercisesCompleted;

    /**
     * Total sets completed across all exercises
     */
    @Min(1)
    private Integer totalSets;

    /**
     * Total reps completed across all exercises
     */
    @Min(1)
    private Integer totalReps;

    /**
     * When the workout was started
     */
    @NotNull
    private Instant workoutStartTime;

    /**
     * When the workout was completed
     */
    @NotNull
    private Instant workoutEndTime;

    /**
     * Additional metadata about the workout
     * e.g., {"difficulty": "intermediate", "category": "upper-body"}
     */
    private Map<String, Object> metadata;

    @Override
    public String getEventType() {
        return "WORKOUT_COMPLETED";
    }

    @Override
    public boolean isValid() {
        return workoutId != null && !workoutId.trim().isEmpty()
                && workoutType != null && !workoutType.trim().isEmpty()
                && durationMinutes != null && durationMinutes > 0
                && exercisesCompleted != null && exercisesCompleted > 0
                && workoutStartTime != null
                && workoutEndTime != null
                && workoutEndTime.isAfter(workoutStartTime);
    }

    /**
     * Calculate workout intensity score for gamification
     */
    public double getIntensityScore() {
        if (durationMinutes == null || totalSets == null) {
            return 0.0;
        }

        // Simple intensity calculation: sets per minute
        return (double) totalSets / durationMinutes;
    }

    /**
     * Check if this workout contributes to streak calculations
     */
    public boolean isStreakEligible() {
        return durationMinutes != null && durationMinutes >= 15; // Minimum 15 minutes
    }
}