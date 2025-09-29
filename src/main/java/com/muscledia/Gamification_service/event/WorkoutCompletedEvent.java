package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

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

    /**
     * Unique identifier for the workout
     */
    //@NotBlank(message = "Workout ID must not be blank")
    @JsonProperty("workoutId")
    private String workoutId;

    /**
     * Type of workout (e.g., "strength", "cardio", "hiit")
     */
    @JsonProperty("workoutType")
    private String workoutType;

    /**
     * Duration of workout in minutes
     */
    @JsonProperty("durationMinutes")
    private Integer durationMinutes;

    @JsonProperty("caloriesBurned")
    private Integer caloriesBurned;

    @JsonProperty("exercisesCompleted")
    private Integer exercisesCompleted;

    @JsonProperty("totalSets")
    private Integer totalSets;

    @JsonProperty("totalReps")
    private Integer totalReps;

    @JsonProperty("totalVolume")
    private Double totalVolume;

    // Muscle Groups (array of strings)
    @JsonProperty("workedMuscleGroups")
    private List<String> workedMuscleGroups;

    /**
     * When the workout was started
     */
    @JsonProperty("workoutStartTime")
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Instant workoutStartTime;

    @JsonProperty("workoutEndTime")
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Instant workoutEndTime;

    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Additional metadata about the workout
     * e.g., {"difficulty": "intermediate", "category": "upper-body"}
     */
    @JsonProperty("metadata")
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
        boolean baseValid = isBaseValid();
        boolean hasWorkoutId = workoutId != null && !workoutId.trim().isEmpty();
        boolean hasWorkoutType = workoutType != null && !workoutType.trim().isEmpty();

        boolean valid = baseValid && hasWorkoutId && hasWorkoutType;

        if (!valid) {
            log.warn("WorkoutCompletedEvent validation failed: userId={}, workoutId={}, workoutType={}",
                    getUserId(), workoutId, workoutType);
        } else {
            log.debug("WorkoutCompletedEvent validation passed for user {}, workout {}",
                    getUserId(), workoutId);
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