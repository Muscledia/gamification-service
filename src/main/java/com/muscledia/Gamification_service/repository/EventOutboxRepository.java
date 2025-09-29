package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.EventOutbox;
import com.muscledia.Gamification_service.model.EventOutbox.EventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing event outbox entries.
 * Supports the Transactional Outbox Pattern for reliable event publishing.
 */
@Repository
public interface EventOutboxRepository extends MongoRepository<EventOutbox, String> {

    /**
     * Find events that are ready to be published
     */
    @Query("{ 'status': 'PENDING' }")
    List<EventOutbox> findPendingEvents();

    /**
     * Find failed events that are ready for retry
     */
    @Query("{ 'status': 'FAILED', " +
            "'attemptCount': { $lt: ?0 }, " +
            "$or: [ " +
            "  { 'nextRetryAt': { $exists: false } }, " +
            "  { 'nextRetryAt': { $lt: ?1 } } " +
            "] }")
    List<EventOutbox> findRetryableFailedEvents(Integer maxAttempts, Instant now);

    /**
     * Find events by status
     */
    List<EventOutbox> findByStatus(EventStatus status);

    /**
     * Find events by user ID and status
     */
    List<EventOutbox> findByUserIdAndStatus(Long userId, EventStatus status);

    /**
     * Find event by original event ID
     */
    Optional<EventOutbox> findByEventId(String eventId);

    /**
     * Count events by status
     */
    long countByStatus(EventStatus status);

    /**
     * Find old processed events for cleanup (older than specified time)
     */
    @Query("{ 'status': 'PUBLISHED', 'publishedAt': { $lt: ?0 } }")
    List<EventOutbox> findOldPublishedEvents(Instant olderThan);

    /**
     * Find dead letter events for manual review
     */
    @Query("{ 'status': 'DEAD_LETTER' }")
    List<EventOutbox> findDeadLetterEvents();

    /**
     * Find events by topic for monitoring
     */
    List<EventOutbox> findByTopic(String topic);

    /**
     * Delete old published events (cleanup)
     */
    void deleteByStatusAndPublishedAtBefore(EventStatus status, Instant before);
}