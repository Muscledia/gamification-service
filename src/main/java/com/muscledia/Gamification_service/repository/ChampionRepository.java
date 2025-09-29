package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.Champion;
import com.muscledia.Gamification_service.model.enums.ChampionCriteriaType;
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
     * Find champions by criteria type
     */
    List<Champion> findByCriteriaType(ChampionCriteriaType criteriaType);

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
     * Find champions by criteria type and difficulty
     */
    List<Champion> findByCriteriaTypeAndBaseDifficultyLessThanEqual(ChampionCriteriaType criteriaType,
            int maxDifficulty);

    /**
     * Find champions containing specific criteria parameter key
     */
    @Query("{ 'criteriaParams.?0' : { $exists: true } }")
    List<Champion> findByCriteriaParamKey(String paramKey);

    /**
     * Find champions with specific criteria parameter value
     */
    @Query("{ 'criteriaParams.?0' : ?1 }")
    List<Champion> findByCriteriaParamKeyAndValue(String paramKey, Object paramValue);

    /**
     * Find champions by criteria type with specific parameter
     */
    @Query("{ 'criteriaType' : ?0, 'criteriaParams.?1' : { $exists: true } }")
    List<Champion> findByCriteriaTypeAndParamKey(ChampionCriteriaType criteriaType, String paramKey);

    /**
     * Find champions by criteria type with specific parameter value
     */
    @Query("{ 'criteriaType' : ?0, 'criteriaParams.?1' : ?2 }")
    List<Champion> findByCriteriaTypeAndParamValue(ChampionCriteriaType criteriaType, String paramKey,
            Object paramValue);

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

    /**
     * Get all criteria types currently in use
     */
    @Query(value = "{}", fields = "{ 'criteriaType' : 1 }")
    List<Champion> findAllCriteriaTypes();
}