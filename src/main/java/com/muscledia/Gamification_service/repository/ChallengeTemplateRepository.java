package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.ChallengeTemplate;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PURPOSE: Data access for challenge templates
 * RESPONSIBILITY: Template persistence operations
 * COUPLING: None - interface only
 */
@Repository
public interface ChallengeTemplateRepository extends MongoRepository<ChallengeTemplate, String> {
    List<ChallengeTemplate> findByTypeAndActive(ChallengeType type, boolean active);

    @Query("{ 'type': ?0, 'active': true }")
    List<ChallengeTemplate> findByTypeAndDifficulty(ChallengeType type, DifficultyLevel difficulty);

    List<ChallengeTemplate> findByActiveTrue();

    @Query("{'type': ?0, 'journeyPhase': ?1, 'active': true}")
    List<ChallengeTemplate> findByTypeAndPhase(ChallengeType type, String phase);
}
