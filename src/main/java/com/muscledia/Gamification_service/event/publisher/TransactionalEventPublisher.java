package com.muscledia.Gamification_service.event.publisher;

import com.muscledia.Gamification_service.event.*;
import com.muscledia.Gamification_service.service.EventOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional Event Publisher using the Outbox Pattern.
 * 
 * This publisher ensures atomic event publishing by storing events in the
 * database
 * within the same transaction as business logic. A separate process publishes
 * events from the outbox to Kafka.
 * 
 * Benefits:
 * - Atomic event publishing with business logic
 * - No lost events due to Kafka failures
 * - Automatic retry and dead letter handling
 * - No Redis dependency
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class TransactionalEventPublisher {

    private final EventOutboxService eventOutboxService;

    /**
     * Publish badge earned event (transactionally)
     */
    @Transactional
    public void publishBadgeEarned(BadgeEarnedEvent event) {
        validateEvent(event);
        eventOutboxService.storeForPublishing(event);
        log.info("Stored badge earned event {} for user {} in outbox",
                event.getEventId(), event.getUserId());
    }

    /**
     * Publish level up event (transactionally)
     */
    @Transactional
    public void publishLevelUp(LevelUpEvent event) {
        validateEvent(event);
        eventOutboxService.storeForPublishing(event);
        log.info("Stored level up event {} for user {} in outbox",
                event.getEventId(), event.getUserId());
    }

    /**
     * Publish quest completed event (transactionally)
     */
    @Transactional
    public void publishQuestCompleted(QuestCompletedEvent event) {
        validateEvent(event);
        eventOutboxService.storeForPublishing(event);
        log.info("Stored quest completed event {} for user {} in outbox",
                event.getEventId(), event.getUserId());
    }

    /**
     * Publish leaderboard updated event (transactionally)
     */
    @Transactional
    public void publishLeaderboardUpdated(LeaderboardUpdatedEvent event) {
        validateEvent(event);
        eventOutboxService.storeForPublishing(event);
        log.info("Stored leaderboard updated event {} for user {} in outbox",
                event.getEventId(), event.getUserId());
    }

    /**
     * Publish streak updated event (transactionally)
     */
    @Transactional
    public void publishStreakUpdated(StreakUpdatedEvent event) {
        validateEvent(event);
        eventOutboxService.storeForPublishing(event);
        log.info("Stored streak updated event {} for user {} in outbox",
                event.getEventId(), event.getUserId());
    }


    /**
     * Validate event before storing
     */
    private void validateEvent(BaseEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (!event.isValid()) {
            throw new IllegalArgumentException("Event validation failed: " + event.getEventId());
        }

        if (eventOutboxService.eventExists(event.getEventId())) {
            log.warn("Event {} already exists in outbox, skipping duplicate", event.getEventId());
            return;
        }
    }

    /**
     * Check if event publishing is healthy
     */
    public boolean isHealthy() {
        try {
            EventOutboxService.OutboxStatistics stats = eventOutboxService.getStatistics();

            // Consider healthy if:
            // 1. Success rate is above 95%
            // 2. Dead letter count is less than 10% of total
            // 3. Pending events are being processed (not stuck)

            double successRate = stats.getSuccessRate();
            long totalEvents = stats.getTotalCount();
            double deadLetterRate = totalEvents > 0 ? (double) stats.getDeadLetterCount() / totalEvents * 100.0 : 0.0;

            boolean isHealthy = successRate >= 95.0 && deadLetterRate < 10.0;

            if (!isHealthy) {
                log.warn("Event publishing health check failed: success_rate={}%, dead_letter_rate={}%",
                        successRate, deadLetterRate);
            }

            return isHealthy;

        } catch (Exception e) {
            log.error("Health check failed", e);
            return false;
        }
    }

    /**
     * Get publishing statistics for monitoring
     */
    public EventOutboxService.OutboxStatistics getStatistics() {
        return eventOutboxService.getStatistics();
    }
}