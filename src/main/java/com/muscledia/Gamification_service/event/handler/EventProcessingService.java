package com.muscledia.Gamification_service.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling event processing idempotency and tracking.
 * Prevents duplicate event processing and maintains event history.
 * 
 * Senior Engineering Note: Uses Redis for distributed idempotency
 * with fallback to in-memory cache for development environments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingService {

    private static final String EVENT_PROCESSING_KEY_PREFIX = "event:processed:";
    private static final Duration EVENT_TRACKING_TTL = Duration.ofDays(7); // Track for 7 days

    private final RedisTemplate<String, String> redisTemplate;

    // Fallback in-memory cache for development
    private final ConcurrentHashMap<String, Boolean> processedEventsCache = new ConcurrentHashMap<>();

    /**
     * Check if an event has already been processed
     */
    public boolean isEventAlreadyProcessed(String eventId) {
        try {
            String key = EVENT_PROCESSING_KEY_PREFIX + eventId;

            // Try Redis first
            if (redisTemplate.hasKey(key)) {
                String value = redisTemplate.opsForValue().get(key);
                boolean processed = "true".equals(value);
                log.debug("Event {} processing status from Redis: {}", eventId, processed);
                return processed;
            }

            // Fallback to in-memory cache
            boolean processed = processedEventsCache.getOrDefault(eventId, false);
            log.debug("Event {} processing status from memory: {}", eventId, processed);
            return processed;

        } catch (Exception e) {
            log.warn("Error checking event processing status for {}, assuming not processed", eventId, e);

            // Fallback to in-memory cache on Redis failure
            return processedEventsCache.getOrDefault(eventId, false);
        }
    }

    /**
     * Mark an event as processed
     */
    public void markEventAsProcessed(String eventId) {
        try {
            String key = EVENT_PROCESSING_KEY_PREFIX + eventId;

            // Store in Redis with TTL
            redisTemplate.opsForValue().set(key, "true", EVENT_TRACKING_TTL);

            // Also store in memory cache as backup
            processedEventsCache.put(eventId, true);

            log.debug("Marked event {} as processed", eventId);

        } catch (Exception e) {
            log.warn("Error marking event {} as processed in Redis, using memory cache", eventId, e);

            // Fallback to in-memory cache
            processedEventsCache.put(eventId, true);
        }
    }

    /**
     * Remove event from processed tracking (for testing purposes)
     */
    public void removeEventFromProcessed(String eventId) {
        try {
            String key = EVENT_PROCESSING_KEY_PREFIX + eventId;
            redisTemplate.delete(key);
            processedEventsCache.remove(eventId);

            log.debug("Removed event {} from processed tracking", eventId);

        } catch (Exception e) {
            log.warn("Error removing event {} from processed tracking", eventId, e);
            processedEventsCache.remove(eventId);
        }
    }

    /**
     * Get the number of processed events in the cache
     */
    public long getProcessedEventCount() {
        try {
            // Try to get count from Redis
            String pattern = EVENT_PROCESSING_KEY_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : processedEventsCache.size();

        } catch (Exception e) {
            log.warn("Error getting processed event count from Redis, using memory cache", e);
            return processedEventsCache.size();
        }
    }

    /**
     * Clear all processed events (for testing purposes)
     */
    public void clearProcessedEvents() {
        try {
            String pattern = EVENT_PROCESSING_KEY_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            processedEventsCache.clear();

            log.info("Cleared all processed events");

        } catch (Exception e) {
            log.warn("Error clearing processed events from Redis, cleared memory cache", e);
            processedEventsCache.clear();
        }
    }
}