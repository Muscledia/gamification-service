package com.muscledia.Gamification_service.event;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
@SuperBuilder(toBuilder = true)
@Slf4j
public class WorkoutCompletedEvent extends BaseEvent {

    private String eventId;

    private Long userId;
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

    private BigDecimal totalVolume;  // Add this field

    private List<String> workedMuscleGroups;  // Add this field

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

    private Instant timestamp;

    /**
     * Additional metadata about the workout
     * e.g., {"difficulty": "intermediate", "category": "upper-body"}
     */
    private Map<String, Object> metadata;

    /**
     * Default constructor for Jackson and Lombok
     */
    public WorkoutCompletedEvent() {
        super();
    }

    @Override
    public String getEventType() {
        return "WORKOUT_COMPLETED"; // Must match "name" in BaseEvent's
    }


    // Simple validation method
    @Override
    public boolean isValid() {
        // Relaxed validation - only check essential fields
        boolean hasUserId = userId != null;
        boolean hasWorkoutId = workoutId != null && !workoutId.trim().isEmpty();
        boolean hasWorkoutType = workoutType != null && !workoutType.trim().isEmpty();
        boolean hasValidTimes = workoutStartTime != null && workoutEndTime != null;

        boolean valid = hasUserId && hasWorkoutId && hasWorkoutType && hasValidTimes;

        // Log validation details for debugging
        if (!valid) {
            log.warn("WorkoutCompletedEvent validation failed: userId={}, workoutId={}, workoutType={}, times=valid:{}",
                    userId, workoutId, workoutType, hasValidTimes);
        } else {
            log.debug("WorkoutCompletedEvent validation passed for user {}, workout {}", userId, workoutId);
        }

        return valid;
    }

    @Override
    public BaseEvent withNewTimestamp() {
        return this.toBuilder()
                .timestamp(Instant.now())
                .build();
    }

    // Add this helper method for intensity calculation
    @Override
    public double getIntensityScore() {
        if (durationMinutes == null || totalSets == null || durationMinutes == 0) {
            return 0.0;
        }
        return (double) totalSets / durationMinutes;
    }

    // Add this helper method for streak eligibility
    @Override
    public boolean isStreakEligible() {
        return durationMinutes != null && durationMinutes >= 15;
    }
}