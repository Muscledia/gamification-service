package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserGamificationProfileRepository extends MongoRepository<UserGamificationProfile, Long> {

    /**
     * Find user profile by user ID
     */
    Optional<UserGamificationProfile> findByUserId(Long userId);

    /**
     * Find users by level
     */
    List<UserGamificationProfile> findByLevel(int level);

    /**
     * Find users with level greater than or equal to specified level
     */
    List<UserGamificationProfile> findByLevelGreaterThanEqual(int minLevel);

    /**
     * Find users with level less than or equal to specified level
     */
    List<UserGamificationProfile> findByLevelLessThanEqual(int maxLevel);

    /**
     * Find users by level range
     */
    List<UserGamificationProfile> findByLevelBetween(int minLevel, int maxLevel);

    /**
     * Find users with points greater than or equal to specified amount
     */
    List<UserGamificationProfile> findByPointsGreaterThanEqual(int minPoints);

    /**
     * Get leaderboard by points (top users)
     */
    List<UserGamificationProfile> findAllByOrderByPointsDesc(Pageable pageable);

    /**
     * Get users ordered by user ID for batch processing
     */
    List<UserGamificationProfile> findAllByOrderByUserId(Pageable pageable);

    /**
     * Get leaderboard by level (highest levels first)
     */
    List<UserGamificationProfile> findAllByOrderByLevelDesc(Pageable pageable);

    /**
     * Find users who leveled up recently
     */
    @Query("{ 'lastLevelUpDate' : { $gte: ?0 } }")
    List<UserGamificationProfile> findUsersWithRecentLevelUp(Instant since);

    /**
     * Find users who have earned a specific badge
     */
    @Query("{ 'earnedBadges.badgeId' : ?0 }")
    List<UserGamificationProfile> findUsersWithBadge(String badgeId);

    /**
     * Find users with specific streak type
     */
    @Query("{ 'streaks.?0' : { $exists: true } }")
    List<UserGamificationProfile> findUsersWithStreakType(String streakType);

    /**
     * Find users with active streak (streak current > 0)
     */
    @Query("{ 'streaks.?0.current' : { $gt: 0 } }")
    List<UserGamificationProfile> findUsersWithActiveStreak(String streakType);

    /**
     * Find users with streak length greater than specified amount
     */
    @Query("{ 'streaks.?0.current' : { $gte: ?1 } }")
    List<UserGamificationProfile> findUsersWithStreakLength(String streakType, int minLength);

    /**
     * Find top users by streak length for a specific streak type
     */
    @Query(value = "{ 'streaks.?0' : { $exists: true } }", sort = "{ 'streaks.?0.current' : -1 }")
    List<UserGamificationProfile> findTopUsersByStreak(String streakType, Pageable pageable);

    /**
     * Find users with longest streak record for a specific streak type
     */
    @Query(value = "{ 'streaks.?0' : { $exists: true } }", sort = "{ 'streaks.?0.longest' : -1 }")
    List<UserGamificationProfile> findTopUsersByLongestStreak(String streakType, Pageable pageable);

    /**
     * Find users ordered by weekly streak
     */
    List<UserGamificationProfile> findAllByOrderByWeeklyStreakDesc(Pageable pageable);

    /**
     * Find users ordered by monthly streak
     */
    List<UserGamificationProfile> findAllByOrderByMonthlyStreakDesc(Pageable pageable);

    /**
     * Find users with active weekly streak
     */
    @Query("{ 'weeklyStreak' : { $gte: 1 } }")
    List<UserGamificationProfile> findUsersWithActiveWeeklyStreak();

    /**
     * Find users with active monthly streak
     */
    @Query("{ 'monthlyStreak' : { $gte: 1 } }")
    List<UserGamificationProfile> findUsersWithActiveMonthlyStreak();

    /**
     * Find users with weekly streak greater than or equal to value
     */
    List<UserGamificationProfile> findByWeeklyStreakGreaterThanEqual(int minStreak);

    /**
     * Find users with monthly streak greater than or equal to value
     */
    List<UserGamificationProfile> findByMonthlyStreakGreaterThanEqual(int minStreak);


    /**
     * Check if user profile exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Count users by level
     */
    long countByLevel(int level);

    /**
     * Count users with minimum points
     */
    long countByPointsGreaterThanEqual(int minPoints);

    /**
     * Find users with badge count greater than specified amount
     */
    @Query("{ 'earnedBadges' : { $size: { $gte: ?0 } } }")
    List<UserGamificationProfile> findUsersWithMinimumBadges(int minBadgeCount);

    /**
     * Find users ordered by recent activity (last level up)
     */
    List<UserGamificationProfile> findAllByOrderByLastLevelUpDateDesc(Pageable pageable);

    /**
     * Get user rank by points (count users with higher points)
     */
    @Query(value = "{ 'points' : { $gt: ?0 } }", count = true)
    long countUsersWithHigherPoints(int userPoints);

    /**
     * Get user rank by level (count users with higher level)
     */
    @Query(value = "{ 'level' : { $gt: ?0 } }", count = true)
    long countUsersWithHigherLevel(int userLevel);


    /**
     * Get ALL users sorted by level (for accurate rank calculation)
     */
    List<UserGamificationProfile> findAllByOrderByLevelDesc();

    /**
     * Get ALL users sorted by points (for accurate rank calculation)
     */
    List<UserGamificationProfile> findAllByOrderByPointsDesc();

    /**
     * Get ALL users sorted by weekly streak (for accurate rank calculation)
     */
    List<UserGamificationProfile> findAllByOrderByWeeklyStreakDesc();

    /**
     * Get ALL users sorted by monthly streak (for accurate rank calculation)
     */
    List<UserGamificationProfile> findAllByOrderByMonthlyStreakDesc();
}