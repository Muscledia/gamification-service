package com.muscledia.Gamification_service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Event triggered when a user completes an individual exercise.
 * This is an INBOUND event that contributes to exercise-specific quest
 * progress.
 * 
 * Senior Engineering Note: More granular than WorkoutCompletedEvent,
 * used for exercise-specific badges and detailed quest tracking.
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class ExerciseCompletedEvent extends BaseEvent {

    /**
     * Name of the exercise completed
     */
    @NotBlank
    private String exerciseName;

    /**
     * Category of exercise (e.g., "chest", "legs", "cardio")
     */
    @NotBlank
    private String exerciseCategory;

    /**
     * Workout this exercise belongs to
     */
    @NotBlank
    private String workoutId;

    /**
     * Number of sets completed
     */
    @Min(1)
    private Integer setsCompleted;

    /**
     * Total reps across all sets
     */
    @Min(1)
    private Integer totalReps;

    /**
     * Weight used (if applicable)
     */
    private Double weight;

    /**
     * Unit for weight measurement
     */
    private String weightUnit;

    /**
     * Duration if it's a time-based exercise
     */
    private Integer durationSeconds;

    /**
     * Distance if it's a distance-based exercise
     */
    private Double distance;

    /**
     * Unit for distance measurement
     */
    private String distanceUnit;

    /**
     * Detailed set information
     * List of {reps: 12, weight: 135, restTime: 60}
     */
    private List<Map<String, Object>> setDetails;

    /**
     * Equipment used for the exercise
     */
    private String equipment;

    /**
     * Additional exercise metadata
     */
    private Map<String, Object> metadata;

    /**
     * Default constructor for Jackson and Lombok
     */
    public ExerciseCompletedEvent() {
        super();
    }

    @Override
    public String getEventType() {
        return "EXERCISE_COMPLETED";
    }

    @Override
    public boolean isValid() {
        return exerciseName != null && !exerciseName.trim().isEmpty()
                && exerciseCategory != null && !exerciseCategory.trim().isEmpty()
                && workoutId != null && !workoutId.trim().isEmpty()
                && setsCompleted != null && setsCompleted > 0
                && totalReps != null && totalReps > 0;
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

    /**
     * Calculate total volume for strength exercises
     */
    public double getVolume() {
        if (weight != null && totalReps != null) {
            return weight * totalReps;
        }
        return 0.0;
    }

    /**
     * Check if this exercise qualifies for specific badge criteria
     */
    public boolean isVolumeExercise() {
        return weight != null && weight > 0;
    }

    /**
     * Check if this is a cardio exercise
     */
    public boolean isCardioExercise() {
        return "cardio".equalsIgnoreCase(exerciseCategory)
                || distance != null
                || durationSeconds != null;
    }

    /**
     * Get intensity level based on exercise parameters
     */
    public String getIntensityLevel() {
        if (isVolumeExercise()) {
            double volume = getVolume();
            if (volume >= 10000)
                return "HIGH";
            if (volume >= 5000)
                return "MEDIUM";
            return "LOW";
        } else if (isCardioExercise() && durationSeconds != null) {
            if (durationSeconds >= 1800)
                return "HIGH"; // 30+ minutes
            if (durationSeconds >= 900)
                return "MEDIUM"; // 15+ minutes
            return "LOW";
        }
        return "UNKNOWN";
    }
}