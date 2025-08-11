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

    /**
     * SERVLET-BASED event processing - blocking operations
     */
    @KafkaListener(
            topics = "${kafka.topics.user-events:user-events}",
            groupId = "${kafka.consumer.group-id:gamification-service-group}"
    )
    public void handleUserRegistered(
            @Payload Object eventObject,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        String eventId = String.format("%s-%s-%d", topic, partition, offset);
        log.info("Processing UserRegisteredEvent [{}]", eventId);

        try {
            // Validate event type
            UserRegisteredEvent event = validateAndCastEvent(eventObject, eventId);

            // Validate event content
            validateEventContent(event, eventId);

            // Process the event
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
            // Don't acknowledge - let Kafka retry
            throw new RuntimeException("Failed to process UserRegisteredEvent", e);
        }
    }

    private UserRegisteredEvent validateAndCastEvent(Object eventObject, String eventId) {
        if (!(eventObject instanceof UserRegisteredEvent)) {
            throw new EventProcessingException(
                    String.format("Invalid event type received [%s]: expected UserRegisteredEvent, got %s",
                            eventId, eventObject.getClass().getSimpleName()));
        }
        return (UserRegisteredEvent) eventObject;
    }

    private void validateEventContent(UserRegisteredEvent event, String eventId) {
        if (!event.isValid()) {
            throw new EventProcessingException(
                    String.format("Invalid event content [%s]: %s", eventId, event));
        }
    }
}
