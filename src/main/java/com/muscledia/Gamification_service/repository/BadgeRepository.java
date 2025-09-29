package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends MongoRepository<Badge, String> {

    /**
     * Find badge by unique name
     */
    Optional<Badge> findByName(String name);

    /**
     * Find all badges of a specific type
     */
    List<Badge> findByBadgeType(BadgeType badgeType);

    /**
     * Find badges by criteria type
     */
    List<Badge> findByCriteriaType(BadgeCriteriaType criteriaType);

    /**
     * Find badges by badge type and criteria type
     */
    List<Badge> findByBadgeTypeAndCriteriaType(BadgeType badgeType, BadgeCriteriaType criteriaType);

    /**
     * Find badges by type ordered by points awarded (ascending)
     */
    List<Badge> findByBadgeTypeOrderByPointsAwarded(BadgeType badgeType);

    /**
     * Find badges with points greater than or equal to specified amount
     */
    List<Badge> findByPointsAwardedGreaterThanEqual(int minPoints);

    /**
     * Find badges by type with minimum points threshold
     */
    List<Badge> findByBadgeTypeAndPointsAwardedGreaterThanEqual(BadgeType badgeType, int minPoints);

    /**
     * Check if badge with name already exists
     */
    boolean existsByName(String name);

    /**
     * Find badges containing specific criteria parameter key
     */
    @Query("{ 'criteriaParams.?0' : { $exists: true } }")
    List<Badge> findByCriteriaParamKey(String paramKey);

    /**
     * Find badges with specific criteria parameter value
     */
    @Query("{ 'criteriaParams.?0' : ?1 }")
    List<Badge> findByCriteriaParamKeyAndValue(String paramKey, Object paramValue);

    /**
     * Find badges by criteria type with specific parameter
     */
    @Query("{ 'criteriaType' : ?0, 'criteriaParams.?1' : { $exists: true } }")
    List<Badge> findByCriteriaTypeAndParamKey(BadgeCriteriaType criteriaType, String paramKey);

    /**
     * Find badges by criteria type with specific parameter value
     */
    @Query("{ 'criteriaType' : ?0, 'criteriaParams.?1' : ?2 }")
    List<Badge> findByCriteriaTypeAndParamValue(BadgeCriteriaType criteriaType, String paramKey, Object paramValue);

    /**
     * Get all badge types currently in use
     */
    @Query(value = "{}", fields = "{ 'badgeType' : 1 }")
    List<Badge> findAllBadgeTypes();

    /**
     * Get all criteria types currently in use
     */
    @Query(value = "{}", fields = "{ 'criteriaType' : 1 }")
    List<Badge> findAllCriteriaTypes();
}