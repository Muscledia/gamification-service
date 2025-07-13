package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.enums.BadgeType;
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
     * Find badges containing specific criteria key
     */
    @Query("{ 'criteria.?0' : { $exists: true } }")
    List<Badge> findByCriteriaKey(String criteriaKey);

    /**
     * Find badges with specific criteria value
     */
    @Query("{ 'criteria.?0' : ?1 }")
    List<Badge> findByCriteriaKeyAndValue(String criteriaKey, Object criteriaValue);

    /**
     * Get all badge types currently in use
     */
    @Query(value = "{}", fields = "{ 'badgeType' : 1 }")
    List<Badge> findAllBadgeTypes();
}