package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
@Slf4j
public class WorkoutCompletedEvent extends BaseEvent {

    @JsonProperty("workoutId")
    private String workoutId;

    @JsonProperty("workoutType")
    private String workoutType;

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

    @JsonProperty("personalRecordsAchieved")
    private Integer personalRecordsAchieved;  // ⬅️ ADDED

    @JsonProperty("workedMuscleGroups")
    private List<String> workedMuscleGroups;

    @JsonProperty("workoutStartTime")
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Instant workoutStartTime;

    @JsonProperty("workoutEndTime")
    @JsonDeserialize(using = TimestampDeserializer.class)
    private Instant workoutEndTime;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public WorkoutCompletedEvent() {
        super();
    }

    @Override
    public String getEventType() {
        return "WORKOUT_COMPLETED";
    }

    @Override
    public boolean isValid() {
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

    @Override
    public double getIntensityScore() {
        if (durationMinutes == null || totalSets == null || durationMinutes == 0) {
            return 0.0;
        }
        return (double) totalSets / durationMinutes;
    }

    @Override
    public boolean isStreakEligible() {
        return durationMinutes != null && durationMinutes >= 15;
    }
}