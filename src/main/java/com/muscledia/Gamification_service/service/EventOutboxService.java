package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.BaseEvent;
import com.muscledia.Gamification_service.model.EventOutbox;
import com.muscledia.Gamification_service.model.EventOutbox.EventStatus;
import com.muscledia.Gamification_service.repository.EventOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing the Transactional Outbox Pattern.
 * 
 * This service ensures atomic event publishing by:
 * 1. Storing events in the database within the same transaction as business
 * logic
 * 2. Having a separate process publish events from the outbox to Kafka
 * 3. Handling retries and dead letter events
 * 
 * NO REDIS DEPENDENCY - Uses MongoDB for persistence
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class EventOutboxService {

    private final EventOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Store an event for publishing (within a transaction)
     * This method should be called within the same transaction as business logic
     */
    @Transactional
    public void storeForPublishing(BaseEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String topic = determineTopicForEvent(event);
            String messageKey = event.getUserId() != null ?
                    event.getUserId().toString() :
                    event.getEventId(); // Fallback to eventId

            EventOutbox outboxEntry = EventOutbox.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .topic(topic)
                    .messageKey(messageKey)
                    .payload(payload)
                    .status(EventStatus.PENDING)
                    .userId(event.getUserId())
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(outboxEntry);

            log.debug("Stored event {} for publishing to topic {}",
                    event.getEventId(), topic);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {} for outbox storage",
                    event.getEventId(), e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }

    /**
     * Get events that are ready to be published
     */
    public List<EventOutbox> getPendingEvents() {
        return outboxRepository.findPendingEvents();
    }

    /**
     * Get failed events that are ready for retry
     */
    public List<EventOutbox> getRetryableFailedEvents() {
        return outboxRepository.findRetryableFailedEvents(3, Instant.now());
    }

    /**
     * Mark event as successfully published
     */
    @Transactional
    public void markAsPublished(String outboxId) {
        Optional<EventOutbox> eventOpt = outboxRepository.findById(outboxId);
        if (eventOpt.isPresent()) {
            EventOutbox event = eventOpt.get();
            event.markAsPublished();
            outboxRepository.save(event);

            log.debug("Marked event {} as published", event.getEventId());
        }
    }

    /**
     * Mark event as failed
     */
    @Transactional
    public void markAsFailed(String outboxId, String errorMessage) {
        Optional<EventOutbox> eventOpt = outboxRepository.findById(outboxId);
        if (eventOpt.isPresent()) {
            EventOutbox event = eventOpt.get();
            event.markAsFailed(errorMessage);
            outboxRepository.save(event);

            log.warn("Marked event {} as failed: {}", event.getEventId(), errorMessage);

            if (event.getStatus() == EventStatus.DEAD_LETTER) {
                log.error("Event {} moved to dead letter after {} attempts",
                        event.getEventId(), event.getAttemptCount());
            }
        }
    }

    /**
     * Mark event as processing to prevent duplicate processing
     */
    @Transactional
    public boolean markAsProcessing(String outboxId) {
        Optional<EventOutbox> eventOpt = outboxRepository.findById(outboxId);
        if (eventOpt.isPresent()) {
            EventOutbox event = eventOpt.get();
            if (event.getStatus() == EventStatus.PENDING ||
                    event.getStatus() == EventStatus.FAILED) {
                event.setStatus(EventStatus.PROCESSING);
                event.setUpdatedAt(Instant.now());
                outboxRepository.save(event);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an event already exists in the outbox
     */
    public boolean eventExists(String eventId) {
        return outboxRepository.findByEventId(eventId).isPresent();
    }

    /**
     * Get outbox statistics for monitoring
     */
    public OutboxStatistics getStatistics() {
        return OutboxStatistics.builder()
                .pendingCount(outboxRepository.countByStatus(EventStatus.PENDING))
                .processingCount(outboxRepository.countByStatus(EventStatus.PROCESSING))
                .publishedCount(outboxRepository.countByStatus(EventStatus.PUBLISHED))
                .failedCount(outboxRepository.countByStatus(EventStatus.FAILED))
                .deadLetterCount(outboxRepository.countByStatus(EventStatus.DEAD_LETTER))
                .build();
    }

    /**
     * Clean up old published events to prevent database bloat
     */
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoffTime = Instant.now().minus(7, ChronoUnit.DAYS);
        List<EventOutbox> oldEvents = outboxRepository.findOldPublishedEvents(cutoffTime);

        if (!oldEvents.isEmpty()) {
            outboxRepository.deleteAll(oldEvents);
            log.info("Cleaned up {} old published events", oldEvents.size());
        }
    }

    /**
     * Get dead letter events for manual review
     */
    public List<EventOutbox> getDeadLetterEvents() {
        return outboxRepository.findDeadLetterEvents();
    }

    /**
     * Manually retry a dead letter event
     */
    @Transactional
    public void retryDeadLetterEvent(String outboxId) {
        Optional<EventOutbox> eventOpt = outboxRepository.findById(outboxId);
        if (eventOpt.isPresent()) {
            EventOutbox event = eventOpt.get();
            if (event.getStatus() == EventStatus.DEAD_LETTER) {
                event.setStatus(EventStatus.PENDING);
                event.setAttemptCount(0);
                event.setErrorMessage(null);
                event.setNextRetryAt(null);
                outboxRepository.save(event);

                log.info("Manually retrying dead letter event {}", event.getEventId());
            }
        }
    }

    /**
     * Determine the appropriate Kafka topic for an event
     */
    private String determineTopicForEvent(BaseEvent event) {
        return switch (event.getEventType()) {
            case "BADGE_EARNED" -> "badge-events";
            case "LEVEL_UP" -> "level-up-events";
            case "QUEST_COMPLETED" -> "quest-events";
            case "LEADERBOARD_UPDATED" -> "leaderboard-events";
            case "STREAK_UPDATED" -> "gamification-events";
            default -> "gamification-events";
        };
    }

    /**
     * Statistics class for monitoring
     */
    @lombok.Value
    @lombok.Builder
    public static class OutboxStatistics {
        long pendingCount;
        long processingCount;
        long publishedCount;
        long failedCount;
        long deadLetterCount;

        public long getTotalCount() {
            return pendingCount + processingCount + publishedCount + failedCount + deadLetterCount;
        }

        public double getSuccessRate() {
            long total = getTotalCount();
            return total > 0 ? (double) publishedCount / total * 100.0 : 0.0;
        }
    }
}