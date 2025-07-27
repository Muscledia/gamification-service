package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
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

    /**
     * Unique identifier for this event instance
     */
    @NotBlank
    @lombok.Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /**
     * Timestamp when the event occurred
     */
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @lombok.Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * User ID associated with this event
     */
    @NotNull
    private Long userId;

    /**
     * Service that generated this event
     */
    @NotBlank
    @lombok.Builder.Default
    private String source = "gamification-service";

    /**
     * Event version for schema evolution
     */
    @NotBlank
    @lombok.Builder.Default
    private String version = "1.0";

    /**
     * Default constructor for Jackson
     */
    protected BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.source = "gamification-service";
        this.version = "1.0";
    }

    /**
     * Constructor with all fields
     */
    protected BaseEvent(String eventId, Instant timestamp, Long userId, String source, String version) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.userId = userId;
        this.source = source != null ? source : "gamification-service";
        this.version = version != null ? version : "1.0";
    }

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
}