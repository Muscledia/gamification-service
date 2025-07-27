package com.muscledia.Gamification_service.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.ExponentialBackOff;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.TransientDataAccessException;

import java.util.function.BiFunction;

/**
 * Comprehensive Kafka error handling configuration.
 * 
 * Provides:
 * - Retry logic with exponential backoff
 * - Dead Letter Queue (DLQ) for failed messages
 * - Differentiated handling for retryable vs non-retryable exceptions
 * - Comprehensive logging and monitoring
 * 
 * No Redis dependency - uses Kafka's built-in retry and DLQ mechanisms
 */
@Configuration
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class KafkaErrorHandlingConfig {

    /**
     * Global error handler for all Kafka listeners
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {

        // Configure exponential backoff: 1s, 2s, 4s, 8s, 16s (max 5 retries)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(300000L); // Max 5 minutes total retry time

        // Create Dead Letter Publishing Recoverer
        DeadLetterPublishingRecoverer deadLetterRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                deadLetterDestinationResolver());

        // Enhance dead letter recoverer with custom headers
        deadLetterRecoverer.setHeadersFunction((consumerRecord, exception) -> {
            var headers = consumerRecord.headers();
            headers.add("dlt-exception-type", exception.getClass().getSimpleName().getBytes());
            headers.add("dlt-exception-message", exception.getMessage().getBytes());
            headers.add("dlt-original-topic", consumerRecord.topic().getBytes());
            headers.add("dlt-original-partition", String.valueOf(consumerRecord.partition()).getBytes());
            headers.add("dlt-original-offset", String.valueOf(consumerRecord.offset()).getBytes());
            headers.add("dlt-failure-timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
            return headers;
        });

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterRecoverer, backOff);

        // Configure which exceptions should NOT be retried (fail immediately)
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class, // Invalid data
                JsonProcessingException.class, // JSON parsing errors
                ClassCastException.class, // Type conversion errors
                NullPointerException.class, // Null pointer errors
                SecurityException.class, // Security violations
                ValidationException.class // Custom validation errors
        );

        // Configure which exceptions SHOULD be retried
        errorHandler.addRetryableExceptions(
                TransientDataAccessException.class, // Database temporary issues
                org.springframework.dao.QueryTimeoutException.class, // DB timeouts
                java.util.concurrent.TimeoutException.class, // General timeouts
                org.springframework.kafka.KafkaException.class // Kafka-specific retryable issues
        );

        // Log retry attempts
        errorHandler.setRetryListeners(
                (record, ex, deliveryAttempt) -> log.warn("Retry attempt {} for topic {} partition {} offset {}: {}",
                        deliveryAttempt,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        ex.getMessage()));

        return errorHandler;
    }

    /**
     * Dead Letter Topic destination resolver
     */
    private BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> deadLetterDestinationResolver() {
        return (record, ex) -> {
            String originalTopic = record.topic();
            String deadLetterTopic = originalTopic + ".DLT"; // Dead Letter Topic suffix

            log.error("Sending message to DLT: topic={}, partition={}, offset={}, exception={}",
                    originalTopic, record.partition(), record.offset(), ex.getMessage());

            // Send to DLT with same partition to maintain ordering
            return new TopicPartition(deadLetterTopic, record.partition());
        };
    }

    /**
     * Custom validation exception for non-retryable validation errors
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}