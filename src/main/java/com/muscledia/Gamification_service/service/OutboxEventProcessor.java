package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.EventOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Background processor for the Transactional Outbox Pattern.
 * 
 * This service:
 * 1. Polls the outbox for pending events
 * 2. Publishes events to Kafka
 * 3. Handles retries and failures
 * 4. Manages dead letter events
 * 5. Cleans up old events
 * 
 * Runs independently of business logic to ensure reliable event delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class OutboxEventProcessor {

    private final EventOutboxService eventOutboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int BATCH_SIZE = 50;
    private static final int KAFKA_TIMEOUT_SECONDS = 10;

    /**
     * Process pending events from the outbox (every 5 seconds)
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        try {
            List<EventOutbox> pendingEvents = eventOutboxService.getPendingEvents();

            if (pendingEvents.isEmpty()) {
                return;
            }

            log.debug("Processing {} pending events from outbox", pendingEvents.size());

            int processedCount = 0;
            int failedCount = 0;

            for (EventOutbox event : pendingEvents) {
                try {
                    if (processEvent(event)) {
                        processedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing outbox event {}: {}",
                            event.getEventId(), e.getMessage());
                    eventOutboxService.markAsFailed(event.getId(), e.getMessage());
                    failedCount++;
                }
            }

            if (processedCount > 0 || failedCount > 0) {
                log.info("Outbox processing completed: {} published, {} failed",
                        processedCount, failedCount);
            }

        } catch (Exception e) {
            log.error("Error during outbox processing", e);
        }
    }

    /**
     * Process failed events that are ready for retry (every 2 minutes)
     */
    @Scheduled(fixedDelay = 120000)
    public void processRetryableFailedEvents() {
        try {
            List<EventOutbox> retryableEvents = eventOutboxService.getRetryableFailedEvents();

            if (retryableEvents.isEmpty()) {
                return;
            }

            log.info("Retrying {} failed events", retryableEvents.size());

            int retrySuccessCount = 0;
            int retryFailedCount = 0;

            for (EventOutbox event : retryableEvents) {
                try {
                    if (processEvent(event)) {
                        retrySuccessCount++;
                        log.info("Successfully retried event {} after {} attempts",
                                event.getEventId(), event.getAttemptCount());
                    } else {
                        retryFailedCount++;
                    }
                } catch (Exception e) {
                    log.error("Retry failed for event {}: {}",
                            event.getEventId(), e.getMessage());
                    eventOutboxService.markAsFailed(event.getId(), e.getMessage());
                    retryFailedCount++;
                }
            }

            log.info("Retry processing completed: {} successful, {} failed",
                    retrySuccessCount, retryFailedCount);

        } catch (Exception e) {
            log.error("Error during retry processing", e);
        }
    }

    /**
     * Clean up old published events (daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldEvents() {
        try {
            eventOutboxService.cleanupOldEvents();
            log.info("Completed outbox cleanup task");
        } catch (Exception e) {
            log.error("Error during outbox cleanup", e);
        }
    }

    /**
     * Log outbox statistics (every 10 minutes)
     */
    @Scheduled(fixedDelay = 600000)
    public void logStatistics() {
        try {
            EventOutboxService.OutboxStatistics stats = eventOutboxService.getStatistics();

            log.info("Outbox Statistics - Total: {}, Pending: {}, Processing: {}, " +
                    "Published: {}, Failed: {}, Dead Letter: {}, Success Rate: {:.2f}%",
                    stats.getTotalCount(),
                    stats.getPendingCount(),
                    stats.getProcessingCount(),
                    stats.getPublishedCount(),
                    stats.getFailedCount(),
                    stats.getDeadLetterCount(),
                    stats.getSuccessRate());

            // Alert if too many dead letter events
            if (stats.getDeadLetterCount() > 10) {
                log.warn("High number of dead letter events detected: {}. " +
                        "Manual intervention may be required.", stats.getDeadLetterCount());
            }

        } catch (Exception e) {
            log.error("Error logging outbox statistics", e);
        }
    }

    /**
     * Process a single event from the outbox
     */
    private boolean processEvent(EventOutbox outboxEvent) {
        // Mark as processing to prevent duplicate processing
        if (!eventOutboxService.markAsProcessing(outboxEvent.getId())) {
            log.debug("Event {} is already being processed or has been processed",
                    outboxEvent.getEventId());
            return false;
        }

        try {
            // Convert JSON payload back to object for Kafka serialization
            Object eventPayload = parseEventPayload(outboxEvent.getPayload());

            // Publish to Kafka with timeout
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                    outboxEvent.getTopic(),
                    outboxEvent.getMessageKey(),
                    eventPayload);

            // Wait for result with timeout
            SendResult<String, Object> result = future.get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Mark as successfully published
            eventOutboxService.markAsPublished(outboxEvent.getId());

            log.debug("Successfully published event {} to topic {} with offset {}",
                    outboxEvent.getEventId(),
                    outboxEvent.getTopic(),
                    result.getRecordMetadata().offset());

            return true;

        } catch (Exception e) {
            log.error("Failed to publish event {} to topic {}: {}",
                    outboxEvent.getEventId(), outboxEvent.getTopic(), e.getMessage());

            eventOutboxService.markAsFailed(outboxEvent.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Parse JSON payload - for now just return as string since
     * KafkaTemplate will handle JSON serialization
     */
    private Object parseEventPayload(String payload) {
        // Since we're using JsonSerializer in KafkaTemplate,
        // we can pass the JSON string directly or parse it back to object
        // For simplicity, return the JSON string
        return payload;
    }

    /**
     * Check if the processor is healthy
     */
    public boolean isHealthy() {
        try {
            EventOutboxService.OutboxStatistics stats = eventOutboxService.getStatistics();

            // Consider unhealthy if there are too many failed or dead letter events
            long totalEvents = stats.getTotalCount();
            if (totalEvents == 0) {
                return true; // No events to process
            }

            double failureRate = (double) (stats.getFailedCount() + stats.getDeadLetterCount()) / totalEvents;
            boolean isHealthy = failureRate < 0.05; // Less than 5% failure rate

            if (!isHealthy) {
                log.warn("Outbox processor health check failed: failure_rate={:.2f}%",
                        failureRate * 100);
            }

            return isHealthy;

        } catch (Exception e) {
            log.error("Outbox processor health check failed", e);
            return false;
        }
    }
}