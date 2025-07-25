package com.muscledia.Gamification_service.config;

import com.muscledia.Gamification_service.event.BaseEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for event-driven gamification processing.
 * 
 * Senior Engineering Design Decisions:
 * 1. Separate topics for inbound/outbound events for clean separation
 * 2. JSON serialization with type information for schema evolution
 * 3. Error handling with dead letter queues and retry mechanisms
 * 4. Performance optimizations for high-throughput scenarios
 * 5. Consumer groups for horizontal scaling
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:gamification-service}")
    private String consumerGroupId;

    // ===============================
    // TOPIC DEFINITIONS
    // ===============================

    /**
     * Inbound Topics - Events consumed from other services
     */
    @Bean
    public NewTopic userActivityTopic() {
        return TopicBuilder.name("user-activity-events")
                .partitions(3) // Allows parallel processing
                .replicas(1) // Increase in production
                .config("retention.ms", "604800000") // 7 days retention
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic workoutTopic() {
        return TopicBuilder.name("workout-events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic personalRecordTopic() {
        return TopicBuilder.name("personal-record-events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days for PRs
                .build();
    }

    /**
     * Outbound Topics - Events published by gamification service
     */
    @Bean
    public NewTopic gamificationEventsTopic() {
        return TopicBuilder.name("gamification-events")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days retention
                .build();
    }

    @Bean
    public NewTopic badgeEventsTopic() {
        return TopicBuilder.name("badge-events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "2592000000")
                .build();
    }

    @Bean
    public NewTopic levelUpEventsTopic() {
        return TopicBuilder.name("level-up-events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "2592000000")
                .build();
    }

    @Bean
    public NewTopic questEventsTopic() {
        return TopicBuilder.name("quest-events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "1209600000") // 14 days
                .build();
    }

    @Bean
    public NewTopic leaderboardEventsTopic() {
        return TopicBuilder.name("leaderboard-events")
                .partitions(2)
                .replicas(1)
                .config("retention.ms", "259200000") // 3 days
                .build();
    }

    /**
     * Dead Letter Queue Topic for failed message processing
     */
    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("gamification-dlq")
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days for debugging
                .build();
    }

    // ===============================
    // PRODUCER CONFIGURATION
    // ===============================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Performance Optimizations
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB batches
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Wait up to 10ms for batching
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

        // Reliability Configuration
        props.put(ProducerConfig.ACKS_CONFIG, "1"); // Leader acknowledgment
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        // JSON Serialization Configuration
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        props.put(JsonSerializer.TYPE_MAPPINGS, getTypeMapping());

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());

        // Set default topic for convenience
        template.setDefaultTopic("gamification-events");

        return template;
    }

    // ===============================
    // CONSUMER CONFIGURATION
    // ===============================

    @Bean
    public ConsumerFactory<String, BaseEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JSON Deserialization Configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.muscledia.Gamification_service.event");
        props.put(JsonDeserializer.TYPE_MAPPINGS, getTypeMapping());
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseEvent.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Consumer Behavior Configuration
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reliability
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // Session and Heartbeat Configuration
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BaseEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, BaseEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Concurrency Configuration
        factory.setConcurrency(3); // Process messages in parallel

        // Acknowledge Configuration
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Error Handling Configuration
        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }

    // ===============================
    // ERROR HANDLING
    // ===============================

    @Bean
    public DefaultErrorHandler errorHandler() {
        // Configure retry with exponential backoff
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3L); // 1 second interval, 3 retries

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(fixedBackOff);

        // Add exception classification - don't retry for certain exceptions
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Define type mappings for JSON serialization/deserialization
     */
    private String getTypeMapping() {
        return "workout:com.muscledia.Gamification_service.event.WorkoutCompletedEvent," +
                "pr:com.muscledia.Gamification_service.event.PersonalRecordEvent," +
                "exercise:com.muscledia.Gamification_service.event.ExerciseCompletedEvent," +
                "streak:com.muscledia.Gamification_service.event.StreakUpdatedEvent," +
                "badge:com.muscledia.Gamification_service.event.BadgeEarnedEvent," +
                "levelup:com.muscledia.Gamification_service.event.LevelUpEvent," +
                "quest:com.muscledia.Gamification_service.event.QuestCompletedEvent," +
                "leaderboard:com.muscledia.Gamification_service.event.LeaderboardUpdatedEvent";
    }
}

/**
 * Configuration constants for topic names and consumer groups
 */
final class KafkaTopics {
    public static final String USER_ACTIVITY_EVENTS = "user-activity-events";
    public static final String WORKOUT_EVENTS = "workout-events";
    public static final String PERSONAL_RECORD_EVENTS = "personal-record-events";
    public static final String GAMIFICATION_EVENTS = "gamification-events";
    public static final String BADGE_EVENTS = "badge-events";
    public static final String LEVEL_UP_EVENTS = "level-up-events";
    public static final String QUEST_EVENTS = "quest-events";
    public static final String LEADERBOARD_EVENTS = "leaderboard-events";
    public static final String DEAD_LETTER_QUEUE = "gamification-dlq";

    private KafkaTopics() {
    } // Utility class
}