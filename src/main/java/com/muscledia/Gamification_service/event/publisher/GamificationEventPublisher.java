package com.muscledia.Gamification_service.event.publisher;

import com.muscledia.Gamification_service.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing gamification events to Kafka topics.
 * 
 * ONLY ENABLED WHEN EVENTS ARE ENABLED
 * For MVP: Disabled by default (no Kafka required)
 * For Production: Enable with EVENTS_ENABLED=true
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class GamificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic constants
    private static final String BADGE_EVENTS_TOPIC = "badge-events";
    private static final String LEVEL_UP_EVENTS_TOPIC = "level-up-events";
    private static final String QUEST_EVENTS_TOPIC = "quest-events";
    private static final String LEADERBOARD_EVENTS_TOPIC = "leaderboard-events";
    private static final String GAMIFICATION_EVENTS_TOPIC = "gamification-events";

    /**
     * Publish badge earned event
     */
    public void publishBadgeEarned(BadgeEarnedEvent event) {
        publishEvent(BADGE_EVENTS_TOPIC, event.getUserId().toString(), event);
        log.info("Published badge earned event: {} for user {}",
                event.getBadgeName(), event.getUserId());
    }

    /**
     * Publish level up event
     */
    public void publishLevelUp(LevelUpEvent event) {
        publishEvent(LEVEL_UP_EVENTS_TOPIC, event.getUserId().toString(), event);
        log.info("Published level up event: Level {} for user {}",
                event.getNewLevel(), event.getUserId());
    }

    /**
     * Publish quest completed event
     */
    public void publishQuestCompleted(QuestCompletedEvent event) {
        publishEvent(QUEST_EVENTS_TOPIC, event.getUserId().toString(), event);
        log.info("Published quest completed event: {} for user {}",
                event.getQuestName(), event.getUserId());
    }

    /**
     * Publish leaderboard updated event
     */
    public void publishLeaderboardUpdated(LeaderboardUpdatedEvent event) {
        publishEvent(LEADERBOARD_EVENTS_TOPIC, event.getUserId().toString(), event);
        log.info("Published leaderboard updated event: {} rank {} for user {}",
                event.getLeaderboardType(), event.getNewRank(), event.getUserId());
    }

    /**
     * Publish generic gamification event
     */
    public void publishGamificationEvent(BaseEvent event) {
        publishEvent(GAMIFICATION_EVENTS_TOPIC, event.getUserId().toString(), event);
        log.info("Published gamification event: {} for user {}",
                event.getEventType(), event.getUserId());
    }

    /**
     * Core method to publish events with error handling
     */
    private void publishEvent(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to publish event to topic {}: {}", topic, throwable.getMessage());
                    handlePublishFailure(topic, key, event, throwable);
                } else {
                    log.debug("Successfully published event to topic {} with offset {}",
                            topic, result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Error publishing event to topic {}: {}", topic, e.getMessage(), e);
            handlePublishFailure(topic, key, event, e);
        }
    }

    /**
     * Handle publish failures - could implement retry logic or dead letter queue
     */
    private void handlePublishFailure(String topic, String key, Object event, Throwable error) {
        log.error("Event publishing failed for topic {} with key {}: {}",
                topic, key, error.getMessage());

        // In a production environment, you might:
        // 1. Store failed events in a database for retry
        // 2. Send to a dead letter queue
        // 3. Implement exponential backoff retry
        // 4. Alert monitoring systems

        // For now, just log the failure
        log.warn("Event lost due to publishing failure. Consider implementing retry mechanism.");
    }

    /**
     * Publish multiple events in batch (for performance optimization)
     */
    public void publishEventsInBatch(String topic, java.util.List<? extends BaseEvent> events) {
        log.info("Publishing {} events in batch to topic {}", events.size(), topic);

        for (BaseEvent event : events) {
            publishEvent(topic, event.getUserId().toString(), event);
        }
    }

    /**
     * Health check method to verify Kafka connectivity
     */
    public boolean isHealthy() {
        try {
            // Send a simple ping message to verify connectivity
            var future = kafkaTemplate.send("health-check", "ping", "pong");
            future.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(5),
                    java.util.concurrent.TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }

}