package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.Champion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChampionRepository extends MongoRepository<Champion, String> {

    /**
     * Find champion by name
     */
    Optional<Champion> findByName(String name);

    /**
     * Find champions by required exercise ID
     */
    List<Champion> findByRequiredExerciseId(String exerciseId);

    /**
     * Find champions by muscle group ID
     */
    List<Champion> findByMuscleGroupId(String muscleGroupId);

    /**
     * Find champions by difficulty level
     */
    List<Champion> findByBaseDifficulty(int difficulty);

    /**
     * Find champions with difficulty less than or equal to specified level
     */
    List<Champion> findByBaseDifficultyLessThanEqual(int maxDifficulty);

    /**
     * Find champions with difficulty greater than or equal to specified level
     */
    List<Champion> findByBaseDifficultyGreaterThanEqual(int minDifficulty);

    /**
     * Find champions by difficulty range
     */
    List<Champion> findByBaseDifficultyBetween(int minDifficulty, int maxDifficulty);

    /**
     * Find champions containing specific criteria key
     */
    @Query("{ 'criteria.?0' : { $exists: true } }")
    List<Champion> findByCriteriaKey(String criteriaKey);

    /**
     * Find champions with specific criteria value
     */
    @Query("{ 'criteria.?0' : ?1 }")
    List<Champion> findByCriteriaKeyAndValue(String criteriaKey, Object criteriaValue);

    /**
     * Find champions ordered by difficulty (easiest first)
     */
    List<Champion> findAllByOrderByBaseDifficultyAsc();

    /**
     * Find champions ordered by difficulty (hardest first)
     */
    List<Champion> findAllByOrderByBaseDifficultyDesc();

    /**
     * Find champions ordered by creation date (newest first)
     */
    List<Champion> findAllByOrderByCreatedAtDesc();

    /**
     * Check if champion with name already exists
     */
    boolean existsByName(String name);

    /**
     * Find champions for specific exercise and difficulty range
     */
    List<Champion> findByRequiredExerciseIdAndBaseDifficultyLessThanEqual(String exerciseId, int maxDifficulty);

    /**
     * Find champions for specific muscle group and difficulty range
     */
    List<Champion> findByMuscleGroupIdAndBaseDifficultyLessThanEqual(String muscleGroupId, int maxDifficulty);

    /**
     * Find all champions that don't require specific exercises (general champions)
     */
    @Query("{ 'requiredExerciseId' : { $exists: false } }")
    List<Champion> findGeneralChampions();

    /**
     * Find exercise-specific champions
     */
    @Query("{ 'requiredExerciseId' : { $exists: true, $ne: null } }")
    List<Champion> findExerciseSpecificChampions();
}