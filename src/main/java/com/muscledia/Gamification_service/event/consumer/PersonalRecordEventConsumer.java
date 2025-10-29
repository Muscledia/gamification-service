package com.muscledia.Gamification_service.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muscledia.Gamification_service.event.PersonalRecordEvent;
import com.muscledia.Gamification_service.event.handler.PersonalRecordEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class PersonalRecordEventConsumer {

    private final PersonalRecordEventHandler personalRecordEventHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "personal-record-events",
            groupId = "gamification-service-pr-group"
    )
    public void handlePersonalRecord(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            Object eventObject = record.value(); // Extract the actual payload

            log.info("🏆 Received PersonalRecord event: class={}", eventObject.getClass().getSimpleName());

            PersonalRecordEvent event = null;

            switch (eventObject) {
                case PersonalRecordEvent personalRecordEvent -> {
                    event = personalRecordEvent;
                }
                case Map map -> {
                    event = objectMapper.convertValue(map, PersonalRecordEvent.class);
                }
                case String s -> {
                    event = objectMapper.readValue(s, PersonalRecordEvent.class);
                }
                default -> {
                    log.error("Unsupported PersonalRecord event type: {}", eventObject.getClass());
                    acknowledgment.acknowledge();
                    return;
                }
            }

            if (event == null || !event.isValid()) {
                log.error("Invalid PersonalRecordEvent received");
                acknowledgment.acknowledge();
                return;
            }

            log.info("🎯 Processing PersonalRecord: user={}, exercise={}, type={}, value={}",
                    event.getUserId(), event.getExerciseName(), event.getRecordType(), event.getNewValue());

            // Process the personal record for achievements
            personalRecordEventHandler.handlePersonalRecord(event);

            acknowledgment.acknowledge();
            log.info("✅ Successfully processed PersonalRecord for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("❌ Failed to process PersonalRecord event: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
