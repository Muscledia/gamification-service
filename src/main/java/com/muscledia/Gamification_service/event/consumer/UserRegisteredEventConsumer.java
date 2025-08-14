package com.muscledia.Gamification_service.event.consumer;


import com.muscledia.Gamification_service.event.UserRegisteredEvent;
import com.muscledia.Gamification_service.exception.EventProcessingException;
import com.muscledia.Gamification_service.service.UserGamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka consumer for UserRegisteredEvent
 * Uses traditional blocking I/O operations (not reactive)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class UserRegisteredEventConsumer {

    private final UserGamificationService userGamificationService;

    @KafkaListener(
            topics = "${kafka.topics.user-events:user-events}",
            groupId = "${kafka.consumer.group-id:gamification-service-group}"
    )
    public void handleUserRegistered(
            @Payload Map<String, Object> eventData,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        String eventId = String.format("%s-%d-%d", topic, partition, offset);
        log.info("Processing UserRegisteredEvent [{}]", eventId);
        log.info("Received event data: {}", eventData);

        try {
            // Convert Map to UserRegisteredEvent
            UserRegisteredEvent event = convertToUserRegisteredEvent(eventData, eventId);

            // Validate event content
            validateEventContent(event, eventId);

            // Process the event
            log.info("Processing user registration for user {} ({})",
                    event.getUserId(), event.getUsername());
            userGamificationService.processUserRegistration(event);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed UserRegisteredEvent [{}] for user {}",
                    eventId, event.getUserId());

        } catch (EventProcessingException e) {
            log.error("Event processing failed [{}]: {}", eventId, e.getMessage());
            acknowledgment.acknowledge(); // Don't retry business logic errors

        } catch (Exception e) {
            log.error("Unexpected error processing event [{}]: {}", eventId, e.getMessage(), e);
            acknowledgment.acknowledge(); // For now, acknowledge to avoid infinite retry
        }
    }

    private void validateEventContent(UserRegisteredEvent event, String eventId) {
        if (!event.isValid()) {
            throw new EventProcessingException(
                    String.format("Invalid event content [%s]: %s", eventId, event));
        }
    }

    /**
     * Convert received object (Map or UserRegisteredEvent) to our local UserRegisteredEvent
     */
    private UserRegisteredEvent convertToUserRegisteredEvent(Map<String, Object> eventData, String eventId) {
        try {
            log.info("Converting event data to UserRegisteredEvent");

            UserRegisteredEvent.UserRegisteredEventBuilder<?, ?> builder = UserRegisteredEvent.builder();

            // Extract required fields
            Object userIdObj = eventData.get("userId");
            if (userIdObj instanceof Number) {
                builder.userId(((Number) userIdObj).longValue());
            } else if (userIdObj instanceof String) {
                builder.userId(Long.parseLong((String) userIdObj));
            } else {
                throw new EventProcessingException("Invalid userId format: " + userIdObj);
            }

            // Extract string fields
            builder.username((String) eventData.get("username"));
            builder.email((String) eventData.get("email"));
            builder.goalType((String) eventData.get("goalType"));
            builder.initialAvatarType((String) eventData.get("initialAvatarType"));
            builder.eventType((String) eventData.get("eventType"));

            // Handle registration date
            Object regDateObj = eventData.get("registrationDate");
            if (regDateObj instanceof String) {
                builder.registrationDate(Instant.parse((String) regDateObj));
            } else if (regDateObj != null) {
                log.warn("Unexpected registrationDate format: {}", regDateObj.getClass());
                builder.registrationDate(Instant.now()); // Fallback
            }

            // Handle timestamp
            Object timestampObj = eventData.get("timestamp");
            if (timestampObj instanceof String) {
                builder.timestamp(Instant.parse((String) timestampObj));
            } else {
                builder.timestamp(Instant.now()); // Fallback
            }

            // Handle user preferences
            Object prefsObj = eventData.get("userPreferences");
            if (prefsObj instanceof Map) {
                builder.userPreferences((Map<String, Object>) prefsObj);
            }

            UserRegisteredEvent event = builder.build();
            log.info("Successfully converted to UserRegisteredEvent: userId={}, username={}",
                    event.getUserId(), event.getUsername());
            return event;

        } catch (Exception e) {
            log.error("Failed to convert event data: {}", e.getMessage(), e);
            throw new EventProcessingException(
                    String.format("Failed to convert event [%s] to UserRegisteredEvent: %s", eventId, e.getMessage()));
        }
    }
}
