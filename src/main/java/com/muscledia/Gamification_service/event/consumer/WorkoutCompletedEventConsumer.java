package com.muscledia.Gamification_service.event.consumer;


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
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class WorkoutCompletedEventConsumer {
    private final WorkoutEventHandler workoutEventHandler;

    /**
     * CORE USER STORY IMPLEMENTATION: Process workout completion for achievements
     *
     * This method implements the acceptance criteria:
     * - Consumes WorkoutCompletedEvent from Kafka
     * - Triggers achievement processing
     * - Updates user gamification profile
     */
    @KafkaListener(
            topics = "${kafka.topics.workout-events:workout-events}",
            groupId = "${kafka.consumer.group-id:gamification-service-group}"
    )
    public void handleWorkoutCompleted(
            @Payload Object eventObject, // CHANGED: Use Object to match factory
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            // ADDED: Cast to WorkoutCompletedEvent
            if (!(eventObject instanceof WorkoutCompletedEvent)) {
                log.warn("Received non-WorkoutCompletedEvent object: {}", eventObject.getClass());
                acknowledgment.acknowledge();
                return;
            }

            WorkoutCompletedEvent event = (WorkoutCompletedEvent) eventObject;

            log.info("USER STORY IMPLEMENTATION: Received WorkoutCompletedEvent for user {} from topic {}",
                    event.getUserId(), topic);

            // Validate event before processing
            if (!event.isValid()) {
                log.error("Invalid WorkoutCompletedEvent received: {}", event);
                acknowledgment.acknowledge();
                return;
            }

            log.info("🏋Processing workout completion for achievements: userId={}, workoutId={}, duration={}min",
                    event.getUserId(), event.getWorkoutId(), event.getDurationMinutes());

            // CORE IMPLEMENTATION: Process achievements
            workoutEventHandler.handleWorkoutCompleted(event);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("USER STORY SUCCESS: Achievements processed for user {} - user record updated",
                    event.getUserId());

        } catch (Exception e) {
            log.error("USER STORY FAILURE: Failed to process WorkoutCompletedEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process workout completion event", e);
        }
    }

    /**
     * Handle consumption errors
     */
    @org.springframework.kafka.annotation.KafkaListener(
            topics = "${kafka.topics.workout-events:workout-events}.DLT",
            groupId = "${kafka.consumer.group-id:gamification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterEvent(
            @Payload WorkoutCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            ConsumerRecord<String, WorkoutCompletedEvent> record) {

        log.error("Dead Letter Queue: WorkoutCompletedEvent for user {} failed all retries. Manual intervention required.",
                event.getUserId());

        // In production, you might:
        // 1. Store in a dead letter table
        // 2. Send alert to monitoring system
        // 3. Create manual review task
    }
}
