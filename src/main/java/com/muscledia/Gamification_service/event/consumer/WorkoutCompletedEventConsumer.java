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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for WorkoutCompletedEvent from workout-service
 *
 * IMPLEMENTS USER STORY: "Earn Achievements"
 *
 * This consumer:
 * 1. Listens for WorkoutCompletedEvents from workout-events topic
 * 2. Processes achievements automatically when workouts are completed
 * 3. Updates user gamification profiles with new achievements
 *
 * Acceptance Criteria Implementation:
 * - GIVEN: User completes workout that satisfies achievement criteria
 * - WHEN: gamification-service consumes WorkoutCompletedEvent
 * - THEN: user record updated to show new achievement
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.duplicate.consumer.enabled", havingValue = "true") // DISABLE by default
public class WorkoutCompletedEventConsumer {
    private final WorkoutEventHandler workoutEventHandler;
    private final ObjectMapper objectMapper;

    /**
     * CORE USER STORY IMPLEMENTATION: Process workout completion for achievements
     *
     * This method implements the acceptance criteria:
     * - Consumes WorkoutCompletedEvent from Kafka
     * - Triggers achievement processing
     * - Updates user gamification profile
     */
    public void handleWorkoutCompleted(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            Object eventObject = record.value(); // Extract the actual payload

            log.info("RAW EVENT RECEIVED: class={}", eventObject.getClass().getSimpleName());

            WorkoutCompletedEvent event = null;

            // HANDLE DIFFERENT EVENT FORMATS
            switch (eventObject) {
                case WorkoutCompletedEvent workoutCompletedEvent -> {
                    event = workoutCompletedEvent;
                    log.info("Direct WorkoutCompletedEvent received");
                }
                case Map map -> {
                    // Convert Map to WorkoutCompletedEvent
                    Map<String, Object> eventMap = (Map<String, Object>) eventObject;
                    event = objectMapper.convertValue(eventMap, WorkoutCompletedEvent.class);
                    log.info("Converted Map to WorkoutCompletedEvent: userId={}", event.getUserId());
                }
                case String s -> {
                    // Parse JSON string
                    event = objectMapper.readValue(s, WorkoutCompletedEvent.class);
                    log.info("Parsed JSON string to WorkoutCompletedEvent");
                }
                default -> {
                    log.error("UNSUPPORTED EVENT TYPE: {}", eventObject.getClass());
                    acknowledgment.acknowledge();
                    return;
                }
            }

            if (event == null) {
                log.error("Failed to convert event object to WorkoutCompletedEvent");
                acknowledgment.acknowledge();
                return;
            }

            log.info("PROCESSING WORKOUT EVENT: userId={}, workoutId={}, duration={}min",
                    event.getUserId(), event.getWorkoutId(), event.getDurationMinutes());

            // Validate event before processing
            if (!event.isValid()) {
                log.error("Invalid WorkoutCompletedEvent received: {}", event);
                acknowledgment.acknowledge();
                return;
            }

            // CORE IMPLEMENTATION: Process achievements
            workoutEventHandler.handleWorkoutCompleted(event);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("GAMIFICATION SUCCESS: Processed workout event for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("GAMIFICATION FAILURE: Failed to process WorkoutCompletedEvent: {}", e.getMessage(), e);
            acknowledgment.acknowledge(); // Acknowledge to prevent infinite retries
        }
    }

    /**
     * Handle consumption errors
     */
    @KafkaListener(
            topics = "${kafka.topics.workout-events:workout-events}.DLT",
            groupId = "${kafka.consumer.group-id:gamification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterEvent(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            Object eventObject = record.value();

            if (eventObject instanceof Map) {
                Map<String, Object> eventMap = (Map<String, Object>) eventObject;
                Long userId = (Long) eventMap.get("userId");

                log.error("Dead Letter Queue: WorkoutCompletedEvent for user {} failed all retries. Manual intervention required.", userId);
            } else {
                log.error("Dead Letter Queue: Unprocessable WorkoutCompletedEvent received: {}", eventObject.getClass());
            }

            acknowledgment.acknowledge();

            // In production, you might:
            // 1. Store in a dead letter table
            // 2. Send alert to monitoring system
            // 3. Create manual review task

        } catch (Exception e) {
            log.error("Failed to process dead letter event: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
