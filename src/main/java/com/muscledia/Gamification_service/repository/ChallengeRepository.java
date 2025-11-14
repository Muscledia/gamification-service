package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * PURPOSE: Define data access operations without implementation details
 * RESPONSIBILITY: Abstract data persistence operations
 * COUPLING: None - interface only
 */
@Repository
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public interface ChallengeRepository extends MongoRepository<Challenge, String> {
    List<Challenge> findByEndDateBefore(Instant endDate);  // For expired challenges
    List<Challenge> findByStartDateAfter(Instant startDate); // For upcoming challenges

    @Query("{'templateId': ?0, 'startDate': {$gte: ?1}, 'endDate': {$lte: ?2}}")
    Optional<Challenge> findByTemplateIdAndDateRange(String templateId, Instant startDate, Instant endDate);

    @Query("{'endDate': {$lt: ?0}}")
    List<Challenge> findExpiredChallenges(Instant cutoffDate);

    List<Challenge> findByTypeAndActiveTrue(ChallengeType type);
    List<Challenge> findByActiveTrue();
    List<Challenge> findByActiveFalse();

    List<Challenge> findByDifficultyLevel(DifficultyLevel difficulty);
    List<Challenge> findByTypeAndDifficultyLevel(ChallengeType type, DifficultyLevel difficulty);

    // Active challenges within time range
    @Query("{'active': true, 'startDate': {$lte: ?0}, 'endDate': {$gte: ?0}}")
    List<Challenge> findActiveChallengesAt(Instant now);

    // Challenges that auto-enroll users
    List<Challenge> findByAutoEnrollTrueAndActiveTrue();

    // Challenges for specific difficulty level
    @Query("{'active': true, 'startDate': {$lte: ?0}, 'endDate': {$gte: ?0}, 'difficulty': ?1}")
    List<Challenge> findActiveChallengesForDifficulty(Instant now, DifficultyLevel difficulty);


    @Query("{'startDate': {$gt: ?0}}")
    List<Challenge> findUpcomingChallenges(Instant now);
}
