package com.muscledia.Gamification_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.event.handler.WorkoutEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * REFACTORED: Signal over Noise
 * Only logs meaningful user events (achievements, level-ups)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class WorkoutCompletedEventConsumer {
    private final WorkoutEventHandler workoutEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.workout-events:workout-events}",
            groupId = "${kafka.consumer.group-id:gamification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleWorkoutCompleted(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            WorkoutCompletedEvent event = deserializeEvent(record.value());

            if (event == null || !event.isValid()) {
                log.warn("Invalid workout event, skipping");
                acknowledgment.acknowledge();
                return;
            }

            // Process - handler will log meaningful events
            workoutEventHandler.handleWorkoutCompleted(event);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process workout event: {}", e.getMessage());
            acknowledgment.acknowledge();
        }
    }

    /**
     * NOISE REDUCTION: Deserialization moved to DEBUG level
     */
    private WorkoutCompletedEvent deserializeEvent(Object eventObject) {
        try {
            return switch (eventObject) {
                case WorkoutCompletedEvent event -> event;
                case Map map -> objectMapper.convertValue(map, WorkoutCompletedEvent.class);
                case String s -> objectMapper.readValue(s, WorkoutCompletedEvent.class);
                default -> {
                    log.debug("Unsupported event type: {}", eventObject.getClass());
                    yield null;
                }
            };
        } catch (Exception e) {
            log.debug("Event deserialization failed: {}", e.getMessage());
            return null;
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.workout-events:workout-events}.DLT",
            groupId = "${kafka.consumer.group-id:gamification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterEvent(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        log.error("Dead letter event - manual intervention required");
        acknowledgment.acknowledge();
    }
}