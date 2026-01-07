package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events in the gamification system.
 *
 * CRITICAL: Includes events gamification-service PRODUCES and CONSUMES
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        // ========== GAMIFICATION-SERVICE PRODUCES (outbound) ==========
        @JsonSubTypes.Type(value = StreakUpdatedEvent.class, name = "STREAK_UPDATED"),
        @JsonSubTypes.Type(value = BadgeEarnedEvent.class, name = "BADGE_EARNED"),
        @JsonSubTypes.Type(value = LevelUpEvent.class, name = "LEVEL_UP"),
        @JsonSubTypes.Type(value = QuestCompletedEvent.class, name = "QUEST_COMPLETED"),
        @JsonSubTypes.Type(value = LeaderboardUpdatedEvent.class, name = "LEADERBOARD_UPDATED"),
        @JsonSubTypes.Type(value = ChallengeStartedEvent.class, name = "CHALLENGE_STARTED"),
        @JsonSubTypes.Type(value = ChallengeProgressEvent.class, name = "CHALLENGE_PROGRESS"),
        @JsonSubTypes.Type(value = ChallengeCompletedEvent.class, name = "CHALLENGE_COMPLETED"),

        // ========== GAMIFICATION-SERVICE CONSUMES (inbound from workout-service) ==========
        @JsonSubTypes.Type(value = WorkoutCompletedEvent.class, name = "WORKOUT_COMPLETED"),
        @JsonSubTypes.Type(value = PersonalRecordEvent.class, name = "PERSONAL_RECORD"),
        @JsonSubTypes.Type(value = ExerciseCompletedEvent.class, name = "EXERCISE_COMPLETED")
})
public abstract class BaseEvent {

    @NotBlank
    @lombok.Builder.Default
    protected String eventId = UUID.randomUUID().toString();

    @NotNull
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @lombok.Builder.Default
    protected Instant timestamp = Instant.now();

    @NotNull
    protected Long userId;

    @NotBlank
    @lombok.Builder.Default
    protected String source = "gamification-service";

    @NotBlank
    @lombok.Builder.Default
    protected String version = "1.0";

    /**
     * Constructor for common fields
     */
    protected BaseEvent(Long userId) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.userId = userId;
        this.source = "gamification-service";
        this.version = "1.0";
    }

    /**
     * Constructor with all fields for builder/deserialization
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

    /**
     * Helper method for intensity calculation
     */
    public abstract double getIntensityScore();

    /**
     * Helper method for streak eligibility
     */
    public abstract boolean isStreakEligible();

    /**
     * Helper method to validate common fields
     */
    protected boolean isBaseValid() {
        return eventId != null && !eventId.trim().isEmpty() && userId != null;
    }
}