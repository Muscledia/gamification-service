package com.muscledia.Gamification_service.event.listener;

import com.muscledia.Gamification_service.event.*;
import com.muscledia.Gamification_service.event.handler.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * Main event listener for processing user activity events.
 * 
 * ONLY ENABLED WHEN EVENTS ARE ENABLED
 * For MVP: Disabled by default (no Kafka required)
 * For Production: Enable with EVENTS_ENABLED=true
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class UserActivityEventListener {

    private final Validator validator;
    private final WorkoutEventHandler workoutEventHandler;
    private final PersonalRecordEventHandler personalRecordEventHandler;
    private final ExerciseEventHandler exerciseEventHandler;
    private final StreakEventHandler streakEventHandler;
    private final EventProcessingService eventProcessingService;

    /**
     * Listen to workout completion events from the workout service
     */
    @KafkaListener(topics = "workout-events", groupId = "gamification-workout-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handleWorkoutEvent(
            @Payload WorkoutCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received workout event: {} from topic: {}, partition: {}, offset: {}",
                event.getEventId(), topic, partition, offset);

        try {
            // Validate event
            if (!validateEvent(event)) {
                log.error("Invalid workout event received: {}", event);
                acknowledgment.acknowledge(); // Don't retry invalid events
                return;
            }

            // Check for duplicate processing (idempotency)
            if (eventProcessingService.isEventAlreadyProcessed(event.getEventId())) {
                log.info("Event {} already processed, skipping", event.getEventId());
                acknowledgment.acknowledge();
                return;
            }

            // Process the event asynchronously
            processWorkoutEventAsync(event);

            // Mark as processed
            eventProcessingService.markEventAsProcessed(event.getEventId());

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            log.info("Successfully processed workout event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error processing workout event: {}", event.getEventId(), e);
            // Don't acknowledge - let Kafka retry
            throw e;
        }
    }

    /**
     * Listen to personal record events
     */
    @KafkaListener(topics = "personal-record-events", groupId = "gamification-pr-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handlePersonalRecordEvent(
            @Payload PersonalRecordEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        log.info("Received PR event: {} for user: {}", event.getEventId(), event.getUserId());

        try {
            if (!validateEvent(event) || eventProcessingService.isEventAlreadyProcessed(event.getEventId())) {
                acknowledgment.acknowledge();
                return;
            }

            processPersonalRecordEventAsync(event);
            eventProcessingService.markEventAsProcessed(event.getEventId());
            acknowledgment.acknowledge();

            log.info("Successfully processed PR event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error processing PR event: {}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Listen to exercise completion events
     */
    @KafkaListener(topics = "user-activity-events", groupId = "gamification-activity-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handleExerciseEvent(
            @Payload ExerciseCompletedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Received exercise event: {} for user: {}", event.getEventId(), event.getUserId());

        try {
            if (!validateEvent(event) || eventProcessingService.isEventAlreadyProcessed(event.getEventId())) {
                acknowledgment.acknowledge();
                return;
            }

            processExerciseEventAsync(event);
            eventProcessingService.markEventAsProcessed(event.getEventId());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing exercise event: {}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Listen to streak update events (can be both inbound and internally generated)
     */
    @KafkaListener(topics = "user-activity-events", groupId = "gamification-streak-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handleStreakEvent(
            @Payload StreakUpdatedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Received streak event: {} for user: {}", event.getEventId(), event.getUserId());

        try {
            if (!validateEvent(event) || eventProcessingService.isEventAlreadyProcessed(event.getEventId())) {
                acknowledgment.acknowledge();
                return;
            }

            processStreakEventAsync(event);
            eventProcessingService.markEventAsProcessed(event.getEventId());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing streak event: {}", event.getEventId(), e);
            throw e;
        }
    }

    // ===============================
    // ASYNC PROCESSING METHODS
    // ===============================

    @Async("taskExecutor")
    public void processWorkoutEventAsync(WorkoutCompletedEvent event) {
        log.debug("Processing workout event asynchronously: {}", event.getEventId());
        workoutEventHandler.handleWorkoutCompleted(event);
    }

    @Async("taskExecutor")
    public void processPersonalRecordEventAsync(PersonalRecordEvent event) {
        log.debug("Processing PR event asynchronously: {}", event.getEventId());
        personalRecordEventHandler.handlePersonalRecord(event);
    }

    @Async("taskExecutor")
    public void processExerciseEventAsync(ExerciseCompletedEvent event) {
        log.debug("Processing exercise event asynchronously: {}", event.getEventId());
        exerciseEventHandler.handleExerciseCompleted(event);
    }

    @Async("taskExecutor")
    public void processStreakEventAsync(StreakUpdatedEvent event) {
        log.debug("Processing streak event asynchronously: {}", event.getEventId());
        streakEventHandler.handleStreakUpdate(event);
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Validate incoming events using Bean Validation
     */
    private boolean validateEvent(BaseEvent event) {
        Set<ConstraintViolation<BaseEvent>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            log.error("Event validation failed for {}: {}",
                    event.getEventId(),
                    violations.stream()
                            .map(ConstraintViolation::getMessage)
                            .toList());
            return false;
        }

        // Custom business validation
        if (!event.isValid()) {
            log.error("Event business validation failed for {}", event.getEventId());
            return false;
        }

        return true;
    }
}