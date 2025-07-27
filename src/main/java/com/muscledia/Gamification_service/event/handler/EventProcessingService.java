package com.muscledia.Gamification_service.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Redis-free Event Processing Service for managing event idempotency.
 * 
 * Uses MongoDB for persistence with Caffeine in-memory cache for performance.
 * This ensures processed events are tracked reliably without Redis dependency.
 * 
 * Provides:
 * - Event deduplication to prevent duplicate processing
 * - Fast in-memory cache for recent events
 * - MongoDB persistence for durability
 * - Automatic cleanup of old processed events
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "true")
public class EventProcessingService {

    private static final String COLLECTION_NAME = "processed_events";
    private static final Duration EVENT_TRACKING_TTL = Duration.ofDays(7);

    private final MongoTemplate mongoTemplate;

    // High-performance in-memory cache for recently processed events
    private final Cache<String, Boolean> processedEventsCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * Check if an event has already been processed
     */
    public boolean isEventAlreadyProcessed(String eventId) {
        try {
            // First check in-memory cache (fastest)
            Boolean cached = processedEventsCache.getIfPresent(eventId);
            if (cached != null) {
                log.debug("Event {} processing status from cache: {}", eventId, cached);
                return cached;
            }

            // Check MongoDB if not in cache
            boolean processed = existsInMongoDB(eventId);

            // Cache the result for future lookups
            processedEventsCache.put(eventId, processed);

            log.debug("Event {} processing status from MongoDB: {}", eventId, processed);
            return processed;

        } catch (Exception e) {
            log.warn("Error checking event processing status for {}, assuming not processed: {}",
                    eventId, e.getMessage());
            return false;
        }
    }

    /**
     * Mark an event as processed
     */
    public void markEventAsProcessed(String eventId) {
        try {
            // Create processed event document
            ProcessedEventDocument document = new ProcessedEventDocument(eventId, Instant.now());

            // Store in MongoDB with upsert to handle duplicates
            Query query = new Query(Criteria.where("eventId").is(eventId));
            Update update = new Update()
                    .set("eventId", eventId)
                    .set("processedAt", document.processedAt)
                    .setOnInsert("createdAt", document.processedAt);

            mongoTemplate.upsert(query, update, COLLECTION_NAME);

            // Cache the result
            processedEventsCache.put(eventId, true);

            log.debug("Marked event {} as processed", eventId);

        } catch (Exception e) {
            log.error("Error marking event {} as processed: {}", eventId, e.getMessage());

            // Fallback to cache-only if MongoDB is unavailable
            processedEventsCache.put(eventId, true);
            log.warn("Using cache-only fallback for event {}", eventId);
        }
    }

    /**
     * Remove event from processed tracking (for testing purposes)
     */
    public void removeEventFromProcessed(String eventId) {
        try {
            Query query = new Query(Criteria.where("eventId").is(eventId));
            mongoTemplate.remove(query, COLLECTION_NAME);
            processedEventsCache.invalidate(eventId);

            log.debug("Removed event {} from processed tracking", eventId);

        } catch (Exception e) {
            log.warn("Error removing event {} from processed tracking: {}", eventId, e.getMessage());
            processedEventsCache.invalidate(eventId);
        }
    }

    /**
     * Get the number of processed events
     */
    public long getProcessedEventCount() {
        try {
            return mongoTemplate.count(new Query(), COLLECTION_NAME);
        } catch (Exception e) {
            log.warn("Error getting processed event count: {}", e.getMessage());
            return processedEventsCache.estimatedSize();
        }
    }

    /**
     * Clear all processed events (for testing purposes)
     */
    public void clearProcessedEvents() {
        try {
            mongoTemplate.remove(new Query(), COLLECTION_NAME);
            processedEventsCache.invalidateAll();

            log.info("Cleared all processed events");

        } catch (Exception e) {
            log.warn("Error clearing processed events: {}", e.getMessage());
            processedEventsCache.invalidateAll();
        }
    }

    /**
     * Clean up old processed events (runs daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldProcessedEvents() {
        try {
            Instant cutoffTime = Instant.now().minus(EVENT_TRACKING_TTL);
            Query query = new Query(Criteria.where("processedAt").lt(cutoffTime));

            long deletedCount = mongoTemplate.remove(query, COLLECTION_NAME).getDeletedCount();

            if (deletedCount > 0) {
                log.info("Cleaned up {} old processed events", deletedCount);
            }

        } catch (Exception e) {
            log.error("Error during processed events cleanup", e);
        }
    }

    /**
     * Get cache statistics for monitoring
     */
    public CacheStatistics getCacheStatistics() {
        var stats = processedEventsCache.stats();
        return new CacheStatistics(
                processedEventsCache.estimatedSize(),
                stats.hitRate(),
                stats.missRate(),
                stats.evictionCount());
    }

    /**
     * Check if an event exists in MongoDB
     */
    private boolean existsInMongoDB(String eventId) {
        Query query = new Query(Criteria.where("eventId").is(eventId));
        return mongoTemplate.exists(query, COLLECTION_NAME);
    }

    /**
     * Warm up cache with recent events
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void warmUpCache() {
        try {
            // Load recent events into cache to improve performance
            Instant recentTime = Instant.now().minus(Duration.ofHours(2));
            Query query = new Query(Criteria.where("processedAt").gte(recentTime));

            var recentEvents = mongoTemplate.find(query, ProcessedEventDocument.class, COLLECTION_NAME);

            int warmedCount = 0;
            for (ProcessedEventDocument event : recentEvents) {
                if (processedEventsCache.getIfPresent(event.eventId) == null) {
                    processedEventsCache.put(event.eventId, true);
                    warmedCount++;
                }
            }

            if (warmedCount > 0) {
                log.debug("Warmed cache with {} recent processed events", warmedCount);
            }

        } catch (Exception e) {
            log.warn("Error warming up processed events cache", e);
        }
    }

    /**
     * Document class for storing processed events in MongoDB
     */
    private static class ProcessedEventDocument {
        public String eventId;
        public Instant processedAt;

        public ProcessedEventDocument() {
        }

        public ProcessedEventDocument(String eventId, Instant processedAt) {
            this.eventId = eventId;
            this.processedAt = processedAt;
        }
    }

    /**
     * Cache statistics for monitoring
     */
    public record CacheStatistics(
            long size,
            double hitRate,
            double missRate,
            long evictionCount) {
    }
}