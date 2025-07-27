package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.response.ApiResponse;
import com.muscledia.Gamification_service.event.handler.EventProcessingService;
import com.muscledia.Gamification_service.event.publisher.TransactionalEventPublisher;
import com.muscledia.Gamification_service.service.EventOutboxService;
import com.muscledia.Gamification_service.service.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check and monitoring endpoints for Event-Driven Architecture
 * components.
 * 
 * Provides comprehensive monitoring of:
 * - Event publishing health
 * - Outbox processing status
 * - Event processing statistics
 * - Cache performance metrics
 * 
 * No Redis dependency - uses MongoDB and in-memory monitoring
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class EventHealthController {

    private final TransactionalEventPublisher eventPublisher;
    private final EventOutboxService eventOutboxService;
    private final OutboxEventProcessor outboxProcessor;
    private final EventProcessingService eventProcessingService;

    /**
     * Overall health check for all EDA components
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventHealth() {
        try {
            Map<String, Object> healthData = new HashMap<>();

            // Check individual component health
            boolean publisherHealthy = eventPublisher.isHealthy();
            boolean processorHealthy = outboxProcessor.isHealthy();

            // Overall health is good if both components are healthy
            boolean overallHealthy = publisherHealthy && processorHealthy;

            healthData.put("overall", overallHealthy ? "UP" : "DOWN");
            healthData.put("publisher", publisherHealthy ? "UP" : "DOWN");
            healthData.put("processor", processorHealthy ? "UP" : "DOWN");
            healthData.put("timestamp", System.currentTimeMillis());

            if (overallHealthy) {
                return ResponseEntity.ok(ApiResponse.success("Event system is healthy", healthData));
            } else {
                return ResponseEntity.status(503)
                        .body(ApiResponse.error("Event system has issues", healthData));
            }

        } catch (Exception e) {
            log.error("Error checking event health", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Health check failed: " + e.getMessage()));
        }
    }

    /**
     * Get detailed outbox statistics
     */
    @GetMapping("/outbox/stats")
    public ResponseEntity<ApiResponse<EventOutboxService.OutboxStatistics>> getOutboxStatistics() {
        try {
            EventOutboxService.OutboxStatistics stats = eventOutboxService.getStatistics();
            return ResponseEntity.ok(ApiResponse.success("Outbox statistics retrieved", stats));

        } catch (Exception e) {
            log.error("Error retrieving outbox statistics", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve outbox statistics: " + e.getMessage()));
        }
    }

    /**
     * Get event processing cache statistics
     */
    @GetMapping("/processing/cache-stats")
    public ResponseEntity<ApiResponse<EventProcessingService.CacheStatistics>> getCacheStatistics() {
        try {
            EventProcessingService.CacheStatistics stats = eventProcessingService.getCacheStatistics();
            return ResponseEntity.ok(ApiResponse.success("Cache statistics retrieved", stats));

        } catch (Exception e) {
            log.error("Error retrieving cache statistics", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve cache statistics: " + e.getMessage()));
        }
    }

    /**
     * Get comprehensive monitoring dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // Outbox statistics
            EventOutboxService.OutboxStatistics outboxStats = eventOutboxService.getStatistics();
            dashboard.put("outbox", Map.of(
                    "total", outboxStats.getTotalCount(),
                    "pending", outboxStats.getPendingCount(),
                    "processing", outboxStats.getProcessingCount(),
                    "published", outboxStats.getPublishedCount(),
                    "failed", outboxStats.getFailedCount(),
                    "deadLetter", outboxStats.getDeadLetterCount(),
                    "successRate", outboxStats.getSuccessRate()));

            // Cache statistics
            EventProcessingService.CacheStatistics cacheStats = eventProcessingService.getCacheStatistics();
            dashboard.put("cache", Map.of(
                    "size", cacheStats.size(),
                    "hitRate", cacheStats.hitRate(),
                    "missRate", cacheStats.missRate(),
                    "evictionCount", cacheStats.evictionCount()));

            // Event processing statistics
            dashboard.put("processing", Map.of(
                    "processedEventCount", eventProcessingService.getProcessedEventCount()));

            // Health indicators
            dashboard.put("health", Map.of(
                    "publisher", eventPublisher.isHealthy(),
                    "processor", outboxProcessor.isHealthy()));

            return ResponseEntity.ok(ApiResponse.success("Event dashboard data retrieved", dashboard));

        } catch (Exception e) {
            log.error("Error retrieving event dashboard data", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve dashboard data: " + e.getMessage()));
        }
    }

    /**
     * Get dead letter events for manual review
     */
    @GetMapping("/dead-letter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeadLetterEvents() {
        try {
            var deadLetterEvents = eventOutboxService.getDeadLetterEvents();

            Map<String, Object> response = new HashMap<>();
            response.put("count", deadLetterEvents.size());
            response.put("events", deadLetterEvents);

            return ResponseEntity.ok(ApiResponse.success("Dead letter events retrieved", response));

        } catch (Exception e) {
            log.error("Error retrieving dead letter events", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve dead letter events: " + e.getMessage()));
        }
    }

    /**
     * System metrics for external monitoring tools
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();

            EventOutboxService.OutboxStatistics stats = eventOutboxService.getStatistics();
            EventProcessingService.CacheStatistics cacheStats = eventProcessingService.getCacheStatistics();

            // Key metrics for alerting
            metrics.put("events_pending", stats.getPendingCount());
            metrics.put("events_failed", stats.getFailedCount());
            metrics.put("events_dead_letter", stats.getDeadLetterCount());
            metrics.put("events_success_rate", stats.getSuccessRate());
            metrics.put("cache_hit_rate", cacheStats.hitRate());
            metrics.put("cache_size", cacheStats.size());
            metrics.put("publisher_healthy", eventPublisher.isHealthy() ? 1 : 0);
            metrics.put("processor_healthy", outboxProcessor.isHealthy() ? 1 : 0);

            return ResponseEntity.ok(ApiResponse.success("Event metrics retrieved", metrics));

        } catch (Exception e) {
            log.error("Error retrieving event metrics", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to retrieve metrics: " + e.getMessage()));
        }
    }
}