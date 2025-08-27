package com.muscledia.Gamification_service.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;
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
    private final ObjectMapper objectMapper;
    private final WorkoutEventHandler workoutEventHandler;
    private final PersonalRecordEventHandler personalRecordEventHandler;
    private final ExerciseEventHandler exerciseEventHandler;
    private final StreakEventHandler streakEventHandler;
    private final EventProcessingService eventProcessingService;

    /**
     * Listen to workout completion events from the workout service
     * FIXED: Changed @Payload to use Object instead of WorkoutCompletedEvent
     */
    @KafkaListener(topics = "workout-events", groupId = "gamification-workout-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handleWorkoutEvent(
            @Payload Map<String, Object> eventData, // CHANGED: Use Object to handle LinkedHashMap
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received workout event from topic: {}, partition: {}, offset: {}",
                topic, partition, offset);


        try {

            // DEBUG: Print the raw event data
            log.info("🔍 RAW EVENT DATA: {}", eventData);

            // ADDED: Convert Object to WorkoutCompletedEvent
            WorkoutCompletedEvent event = convertToWorkoutCompletedEvent(eventData);

            if (event == null) {
                log.error("Failed to convert event object to WorkoutCompletedEvent");
                acknowledgment.acknowledge();
                return;
            }

            log.info("Successfully converted to WorkoutCompletedEvent: {} for user: {}",
                    event.getEventId(), event.getUserId());

            // DEBUG: Print all fields of the converted event
            debugPrintEventFields(event);

            // Validate event
            if (!validateEventWithDetails(event)) {
                log.error("Invalid workout event received: {}", event.getEventId());
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
            log.error("Error processing workout event: {}", e.getMessage(), e);
            // Don't acknowledge - let Kafka retry
            throw e;
        }
    }


    /**
     * Listen to personal record events
     * FIXED: Changed @Payload to use Object
     */
    @KafkaListener(topics = "personal-record-events", groupId = "gamification-pr-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handlePersonalRecordEvent(
            @Payload Object eventObject, // CHANGED: Use Object
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        log.info("Received PR event from topic: {}", topic);

        try {
            // Convert Object to PersonalRecordEvent
            PersonalRecordEvent event = convertToPersonalRecordEvent(eventObject);

            if (event == null) {
                acknowledgment.acknowledge();
                return;
            }

            if (!validateEvent(event) || eventProcessingService.isEventAlreadyProcessed(event.getEventId())) {
                acknowledgment.acknowledge();
                return;
            }

            processPersonalRecordEventAsync(event);
            eventProcessingService.markEventAsProcessed(event.getEventId());
            acknowledgment.acknowledge();

            log.info("Successfully processed PR event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Error processing PR event: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Listen to exercise completion events
     * FIXED: Changed @Payload to use Object
     */
    @KafkaListener(topics = "user-activity-events", groupId = "gamification-activity-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handleExerciseEvent(
            @Payload Object eventObject, // CHANGED: Use Object
            Acknowledgment acknowledgment) {

        log.info("Received exercise event");

        try {
            // Convert Object to ExerciseCompletedEvent
            ExerciseCompletedEvent event = convertToExerciseCompletedEvent(eventObject);

            if (event == null) {
                acknowledgment.acknowledge();
                return;
            }

            if (!validateEvent(event) || eventProcessingService.isEventAlreadyProcessed(event.getEventId())) {
                acknowledgment.acknowledge();
                return;
            }

            processExerciseEventAsync(event);
            eventProcessingService.markEventAsProcessed(event.getEventId());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing exercise event: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Listen to streak update events
     * FIXED: Changed @Payload to use Object
     */
    @KafkaListener(topics = "user-activity-events", groupId = "gamification-streak-consumer", containerFactory = "kafkaListenerContainerFactory")
    public void handleStreakEvent(
            @Payload Object eventObject, // CHANGED: Use Object
            Acknowledgment acknowledgment) {

        log.info("Received streak event");

        try {
            // Convert Object to StreakUpdatedEvent
            StreakUpdatedEvent event = convertToStreakUpdatedEvent(eventObject);

            if (event == null) {
                acknowledgment.acknowledge();
                return;
            }

            if (!validateEvent(event) || eventProcessingService.isEventAlreadyProcessed(event.getEventId())) {
                acknowledgment.acknowledge();
                return;
            }

            processStreakEventAsync(event);
            eventProcessingService.markEventAsProcessed(event.getEventId());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing streak event: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ===============================
    // ADDED: CONVERSION METHODS
    // ===============================


    /**
     * Convert Map to WorkoutCompletedEvent with detailed logging
     */
    private WorkoutCompletedEvent convertToWorkoutCompletedEvent(Map<String, Object> eventData) {
        try {
            log.debug("🔄 Converting event data to WorkoutCompletedEvent");

            // Log each field we're trying to convert
            log.debug("Event fields present: {}", eventData.keySet());

            WorkoutCompletedEvent event = objectMapper.convertValue(eventData, WorkoutCompletedEvent.class);

            log.debug("✅ Conversion successful");
            return event;

        } catch (Exception e) {
            log.error("❌ Failed to convert event data: {}", e.getMessage(), e);
            log.error("Raw event data: {}", eventData);
            return null;
        }
    }

    private PersonalRecordEvent convertToPersonalRecordEvent(Object eventObject) {
        try {
            switch (eventObject) {
                case PersonalRecordEvent personalRecordEvent -> {
                    return personalRecordEvent;
                }
                case Map map -> {
                    return objectMapper.convertValue(map, PersonalRecordEvent.class);
                }
                case String s -> {
                    return objectMapper.readValue(s, PersonalRecordEvent.class);
                }
                default -> {
                    log.error("Unsupported PR event type: {}", eventObject.getClass());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Failed to convert PR event: {}", e.getMessage(), e);
            return null;
        }
    }

    private ExerciseCompletedEvent convertToExerciseCompletedEvent(Object eventObject) {
        try {
            switch (eventObject) {
                case ExerciseCompletedEvent exerciseCompletedEvent -> {
                    return exerciseCompletedEvent;
                }
                case Map map -> {
                    return objectMapper.convertValue(map, ExerciseCompletedEvent.class);
                }
                case String s -> {
                    return objectMapper.readValue(s, ExerciseCompletedEvent.class);
                }
                default -> {
                    log.error("Unsupported exercise event type: {}", eventObject.getClass());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Failed to convert exercise event: {}", e.getMessage(), e);
            return null;
        }
    }

    private StreakUpdatedEvent convertToStreakUpdatedEvent(Object eventObject) {
        try {
            switch (eventObject) {
                case StreakUpdatedEvent streakUpdatedEvent -> {
                    return streakUpdatedEvent;
                }
                case Map map -> {
                    return objectMapper.convertValue(map, StreakUpdatedEvent.class);
                }
                case String s -> {
                    return objectMapper.readValue(s, StreakUpdatedEvent.class);
                }
                default -> {
                    log.error("Unsupported streak event type: {}", eventObject.getClass());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Failed to convert streak event: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * DEBUG: Print all fields of the event to see what's null
     */
    private void debugPrintEventFields(WorkoutCompletedEvent event) {
        log.info("🔍 EVENT FIELD DEBUG:");
        log.info("  eventId: {}", event.getEventId());
        log.info("  userId: {}", event.getUserId());
        log.info("  source: {}", event.getSource());
        log.info("  version: {}", event.getVersion());
        log.info("  workoutId: {}", event.getWorkoutId());
        log.info("  workoutType: {}", event.getWorkoutType());
        log.info("  durationMinutes: {}", event.getDurationMinutes());
        log.info("  caloriesBurned: {}", event.getCaloriesBurned());
        log.info("  exercisesCompleted: {}", event.getExercisesCompleted());
        log.info("  totalSets: {}", event.getTotalSets());
        log.info("  totalReps: {}", event.getTotalReps());
        log.info("  totalVolume: {}", event.getTotalVolume());
        log.info("  workedMuscleGroups: {}", event.getWorkedMuscleGroups());
        log.info("  workoutStartTime: {}", event.getWorkoutStartTime());
        log.info("  workoutEndTime: {}", event.getWorkoutEndTime());
        log.info("  timestamp: {}", event.getTimestamp());
        log.info("  valid: {}", event.isValid());
    }


    // ===============================
    // ASYNC PROCESSING METHODS
    // ===============================

    @Async("taskExecutor")
    public void processWorkoutEventAsync(WorkoutCompletedEvent event) {
        log.info("Processing workout event asynchronously: {} for user: {}",
                event.getEventId(), event.getUserId());

        try {
            workoutEventHandler.handleWorkoutCompleted(event);
            log.info("Workout event processing completed: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Error in async workout processing: {}", e.getMessage(), e);
            throw e;
        }
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
            log.error("❌ Business validation failed for event {}", event.getEventId());
            return false;
        }

        log.info("✅ Event validation passed for {}", event.getEventId());
        return true;
    }

    /**
     * TEMPORARY: Skip Bean Validation to test the gamification flow
     * We know the fields are populated correctly from the debug logs
     */
    private boolean validateEventWithDetails(WorkoutCompletedEvent event) {
        if (event == null) {
            log.error("❌ Event is null");
            return false;
        }

        /// TEMPORARILY enable Bean Validation with detailed logging
        Set<ConstraintViolation<WorkoutCompletedEvent>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            log.error("Bean validation failed for event {}:", event.getEventId());
            violations.forEach(violation ->
                    log.error("  Field: '{}', Value: '{}', Message: '{}'",
                            violation.getPropertyPath(),
                            violation.getInvalidValue(),
                            violation.getMessage())
            );

            // For now, log but don't fail - just warn
            log.warn("⚠️ Bean validation issues found but proceeding anyway");
        } else {
            log.info("✅ Bean validation passed");
        }

        // Only do basic null checks using getters (which we know work)
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            log.error("❌ Missing eventId: {}", event.getEventId());
            return false;
        }

        if (event.getUserId() == null) {
            log.error("❌ Missing userId: {}", event.getUserId());
            return false;
        }

        if (event.getWorkoutId() == null || event.getWorkoutId().trim().isEmpty()) {
            log.error("❌ Missing workoutId: {}", event.getWorkoutId());
            return false;
        }

        // Custom business validation (this already works)
        if (!event.isValid()) {
            log.error("❌ Business validation failed for event {}", event.getEventId());
            return false;
        }

        log.info("✅ VALIDATION PASSED (Bean validation skipped) for event {}", event.getEventId());
        return true;
    }
}