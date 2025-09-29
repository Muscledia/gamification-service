package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events in the gamification system.
 * Provides common fields and serialization configuration.
 * 
 * Design Decision: Using @SuperBuilder for builder pattern inheritance
 * while maintaining compatibility with abstract classes.
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WorkoutCompletedEvent.class, name = "WORKOUT_COMPLETED"),
        @JsonSubTypes.Type(value = PersonalRecordEvent.class, name = "PERSONAL_RECORD"),
        @JsonSubTypes.Type(value = ExerciseCompletedEvent.class, name = "EXERCISE_COMPLETED"),
        @JsonSubTypes.Type(value = StreakUpdatedEvent.class, name = "STREAK_UPDATED"),
        @JsonSubTypes.Type(value = BadgeEarnedEvent.class, name = "BADGE_EARNED"),
        @JsonSubTypes.Type(value = LevelUpEvent.class, name = "LEVEL_UP"),
        @JsonSubTypes.Type(value = QuestCompletedEvent.class, name = "QUEST_COMPLETED"),
        @JsonSubTypes.Type(value = LeaderboardUpdatedEvent.class, name = "LEADERBOARD_UPDATED")
})
public abstract class BaseEvent {

    @JsonProperty("userId")
    private Long userId;
    /**
     * Unique identifier for this event instance
     */
    @NotBlank(message = "Event ID must not be blank")
    @JsonProperty("eventId")
    private String eventId;

    /**
     * Timestamp when the event occurred
     */
    @JsonProperty("timestamp")
    @JsonDeserialize(using = InstantStringDeserializer.class) // Different deserializer for ISO strings
    private Instant timestamp;

    /**
     * Service that generated this event
     */
    @JsonProperty("source")
    private String source;

    /**
     * Event version for schema evolution
     */
    @JsonProperty("version")
    private String version = "1.0";


    /**
     * Get the event type for routing and processing
     */
    public abstract String getEventType();

    /**
     * Validate event-specific business rules
     */
    public abstract boolean isValid();

    /**
     * Create a copy of this event with updated timestamp (for retries)
     */
    public abstract BaseEvent withNewTimestamp();

    // Add this helper method for intensity calculation
    public abstract double getIntensityScore();

    // Add this helper method for streak eligibility
    public abstract boolean isStreakEligible();

    /**
     * Helper method to validate common fields
     */
    protected boolean isBaseValid() {
        return eventId != null && !eventId.trim().isEmpty() &&
                userId != null;
    }
}