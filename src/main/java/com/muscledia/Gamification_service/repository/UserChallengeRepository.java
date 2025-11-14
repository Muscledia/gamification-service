package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * PURPOSE: Data access for UserChallenge entities
 * RESPONSIBILITY: MongoDB operations for user challenge progress
 */
@Repository
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public interface UserChallengeRepository extends MongoRepository<UserChallenge, String> {
    List<UserChallenge> findByUserId(Long userId);
    List<UserChallenge> findByUserIdAndStatus(Long userId, ChallengeStatus status);

    Optional<UserChallenge> findByUserIdAndChallengeId(Long userId, String challengeId);

    @Query("{'userId': ?0, 'status': 'ACTIVE'}")
    List<UserChallenge> findActiveByUserId(Long userId);

    @Query("{'userId': ?0, 'status': 'COMPLETED'}")
    List<UserChallenge> findCompletedByUserId(Long userId);

    @Query("{'expiresAt': {$lt: ?0}, 'status': 'ACTIVE'}")
    List<UserChallenge> findExpiredActiveChallenges(Instant now);

    // FIX: Add the missing method used in ChallengeScheduler
    @Query("{'status': 'ACTIVE', 'expiresAt': {$lt: ?#{T(java.time.Instant).now()}}}")
    List<UserChallenge> findExpiredActive();

    // Check if user already started a challenge
    boolean existsByUserIdAndChallengeIdAndStatus(Long userId, String challengeId, ChallengeStatus status);

    // Additional useful methods for challenge management
    @Query("{'challengeId': ?0}")
    List<UserChallenge> findByChallengeId(String challengeId);

    @Query("{'status': ?0}")
    List<UserChallenge> findByStatus(ChallengeStatus status);

    // Count active challenges for a user
    @Query(value = "{'userId': ?0, 'status': 'ACTIVE'}", count = true)
    long countActiveByUserId(Long userId);

    @Query("{'userId': ?0, 'startedAt': {'$gte': ?1}}")
    List<UserChallenge> findByUserIdAndStartedAtAfter(Long userId, Instant after);
}
