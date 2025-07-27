package com.muscledia.Gamification_service.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

/**
 * MongoDB document for storing events that need to be published to Kafka.
 * Implements the Transactional Outbox Pattern for atomic event publishing.
 * 
 * This ensures that events are published atomically with database transactions,
 * preventing lost events and maintaining data consistency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "event_outbox")
public class EventOutbox {

    @Id
    private String id;

    /**
     * The original event ID from the domain event
     */
    @Indexed
    private String eventId;

    /**
     * Type of event for routing
     */
    @Indexed
    private String eventType;

    /**
     * Kafka topic where this event should be published
     */
    private String topic;

    /**
     * Kafka message key (usually userId)
     */
    private String messageKey;

    /**
     * JSON payload of the event
     */
    private String payload;

    /**
     * Current status of the event
     */
    @Indexed
    private EventStatus status;

    /**
     * User ID associated with this event
     */
    @Indexed
    private Long userId;

    /**
     * Number of processing attempts
     */
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Maximum number of retry attempts
     */
    @Builder.Default
    private Integer maxAttempts = 3;

    /**
     * Error message if processing failed
     */
    private String errorMessage;

    /**
     * When the event was created
     */
    @CreatedDate
    @Indexed
    private Instant createdAt;

    /**
     * When the event was last updated
     */
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * When the event was successfully published
     */
    private Instant publishedAt;

    /**
     * Next retry time for failed events
     */
    @Indexed
    private Instant nextRetryAt;

    /**
     * Event status enum
     */
    public enum EventStatus {
        PENDING,
        PROCESSING,
        PUBLISHED,
        FAILED,
        DEAD_LETTER
    }

    /**
     * Check if this event can be retried
     */
    public boolean canRetry() {
        return status == EventStatus.FAILED &&
                attemptCount < maxAttempts &&
                (nextRetryAt == null || nextRetryAt.isBefore(Instant.now()));
    }

    /**
     * Increment attempt count and set next retry time
     */
    public void incrementAttempt() {
        this.attemptCount++;
        // Exponential backoff: 1min, 5min, 15min
        long backoffMinutes = (long) Math.pow(5, attemptCount - 1);
        this.nextRetryAt = Instant.now().plusSeconds(backoffMinutes * 60);
    }

    /**
     * Mark as successfully published
     */
    public void markAsPublished() {
        this.status = EventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * Mark as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
        incrementAttempt();

        if (attemptCount >= maxAttempts) {
            this.status = EventStatus.DEAD_LETTER;
        }
    }
}