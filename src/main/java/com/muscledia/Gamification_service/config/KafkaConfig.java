package com.muscledia.Gamification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for event-driven gamification processing.
 * 
 * ONLY ENABLED WHEN EVENTS ARE ENABLED
 * For MVP: Disabled by default to prevent startup issues
 * For Production: Enable with EVENTS_ENABLED=true
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
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

        // Reliability Configuration
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicates
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Performance Configuration
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB batches
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Wait up to 10ms for batching
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

        // Timeout Configuration
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

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
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Configuration
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // SIMPLE FIX: Trust all packages (less secure but works)
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // JSON Deserialization Configuration
        // Disable type info headers to avoid class name conflicts
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        props.put(JsonDeserializer.TYPE_MAPPINGS, getTypeMapping());

        // Consumer Reliability Configuration
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // Only read committed messages

        // Performance Configuration
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // Process 10 records at a time
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // Session Configuration
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // UPDATED: Support WorkoutCompletedEvent by using Object as the value type
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 consumer threads
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(errorHandler());

        // Batch processing (optional)
        factory.setBatchListener(false); // Set to true if you want batch processing

        return factory;
    }

    // ===============================
    // ERROR HANDLING
    // ===============================

    @Bean
    public DefaultErrorHandler errorHandler() {
        // Exponential backoff: start with 1s, max 30s, multiplier 2.0
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(30000L); // Max 30 seconds between retries
        backOff.setMaxElapsedTime(300000L); // Total retry time: 5 minutes

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);

        // Configure Dead Letter Topic (optional)
        // errorHandler.setDeadLetterPublishingRecoverer(deadLetterPublishingRecoverer());

        // Don't retry certain exceptions
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );

        return errorHandler;
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Define type mappings for JSON serialization/deserialization
     */
    private String getTypeMapping() {
        return  "userRegistered:com.muscledia.Gamification_service.event.UserRegisteredEvent," +
                "workout:com.muscledia.Gamification_service.event.WorkoutCompletedEvent";
    }
}

/**
 * Configuration constants for topic names and consumer groups
 */
final class KafkaTopics {
    public static final String USER_EVENTS = "user-events";
    public static final String WORKOUT_EVENTS = "workout-events";
    public static final String PERSONAL_RECORD_EVENTS = "personal-record-events";

    // Output topics (created by gamification service)
    public static final String GAMIFICATION_EVENTS = "gamification-events";
    public static final String BADGE_EVENTS = "badge-events";
    public static final String LEVEL_UP_EVENTS = "level-up-events";
    public static final String QUEST_EVENTS = "quest-events";
    public static final String LEADERBOARD_EVENTS = "leaderboard-events";
    public static final String DEAD_LETTER_QUEUE = "gamification-dlq";

    private KafkaTopics() {
    } // Utility class
}