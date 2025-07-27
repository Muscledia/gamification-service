package com.muscledia.Gamification_service.service.cache;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import com.muscledia.Gamification_service.dto.response.LeaderboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

/**
 * High-performance caching service for leaderboards - REDIS ONLY
 * 
 * This service is only enabled when Redis is available.
 * For MVP (no Redis), use SimpleMVPCacheService instead.
 * 
 * Implements cache warming, invalidation strategies, and fallback mechanisms.
 * Uses multi-layered caching strategy with Redis for distributed caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.cost-optimization.redis-enabled", havingValue = "true")
public class LeaderboardCacheService {

    private final UserGamificationProfileRepository userProfileRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Cache key prefixes
    private static final String POINTS_LEADERBOARD_KEY = "leaderboard:points:";
    private static final String LEVEL_LEADERBOARD_KEY = "leaderboard:level:";
    private static final String STREAK_LEADERBOARD_KEY = "leaderboard:streak:";
    private static final String USER_RANK_KEY = "rank:user:";

    // Cache TTL values
    private static final Duration LEADERBOARD_TTL = Duration.ofMinutes(15);
    private static final Duration USER_RANK_TTL = Duration.ofMinutes(5);

    /**
     * Get cached points leaderboard with fallback to database
     */
    @Cacheable(value = "pointsLeaderboard", key = "#limit")
    public List<LeaderboardResponse> getPointsLeaderboard(int limit) {
        log.debug("Fetching points leaderboard for {} users", limit);

        try {
            // Try Redis cache first
            String cacheKey = POINTS_LEADERBOARD_KEY + limit;
            @SuppressWarnings("unchecked")
            List<LeaderboardResponse> cached = (List<LeaderboardResponse>) redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("Points leaderboard cache hit for limit {}", limit);
                return cached;
            }

            // Cache miss - fetch from database
            log.debug("Points leaderboard cache miss for limit {}", limit);
            List<LeaderboardResponse> leaderboard = fetchPointsLeaderboardFromDatabase(limit);

            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, leaderboard, LEADERBOARD_TTL);

            return leaderboard;

        } catch (Exception e) {
            log.error("Error fetching points leaderboard: {}", e.getMessage());
            // Fallback to database
            return fetchPointsLeaderboardFromDatabase(limit);
        }
    }

    /**
     * Get cached level leaderboard
     */
    @Cacheable(value = "levelLeaderboard", key = "#limit")
    public List<LeaderboardResponse> getLevelLeaderboard(int limit) {
        log.debug("Fetching level leaderboard for {} users", limit);

        try {
            String cacheKey = LEVEL_LEADERBOARD_KEY + limit;
            @SuppressWarnings("unchecked")
            List<LeaderboardResponse> cached = (List<LeaderboardResponse>) redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("Level leaderboard cache hit for limit {}", limit);
                return cached;
            }

            List<LeaderboardResponse> leaderboard = fetchLevelLeaderboardFromDatabase(limit);
            redisTemplate.opsForValue().set(cacheKey, leaderboard, LEADERBOARD_TTL);

            return leaderboard;

        } catch (Exception e) {
            log.error("Error fetching level leaderboard: {}", e.getMessage());
            return fetchLevelLeaderboardFromDatabase(limit);
        }
    }

    /**
     * Get cached streak leaderboard
     */
    @Cacheable(value = "streakLeaderboard", key = "#streakType + '_' + #limit")
    public List<LeaderboardResponse> getStreakLeaderboard(String streakType, int limit) {
        log.debug("Fetching {} streak leaderboard for {} users", streakType, limit);

        try {
            String cacheKey = STREAK_LEADERBOARD_KEY + streakType + ":" + limit;
            @SuppressWarnings("unchecked")
            List<LeaderboardResponse> cached = (List<LeaderboardResponse>) redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("Streak leaderboard cache hit for {} limit {}", streakType, limit);
                return cached;
            }

            List<LeaderboardResponse> leaderboard = fetchStreakLeaderboardFromDatabase(streakType, limit);
            redisTemplate.opsForValue().set(cacheKey, leaderboard, LEADERBOARD_TTL);

            return leaderboard;

        } catch (Exception e) {
            log.error("Error fetching {} streak leaderboard: {}", streakType, e.getMessage());
            return fetchStreakLeaderboardFromDatabase(streakType, limit);
        }
    }

    /**
     * Get user's cached rank in points leaderboard
     */
    public Long getUserPointsRank(Long userId) {
        try {
            String cacheKey = USER_RANK_KEY + "points:" + userId;
            String cachedRank = (String) redisTemplate.opsForValue().get(cacheKey);

            if (cachedRank != null) {
                return Long.parseLong(cachedRank);
            }

            // Calculate and cache rank
            Long rank = calculateUserPointsRank(userId);
            redisTemplate.opsForValue().set(cacheKey, rank.toString(), USER_RANK_TTL);

            return rank;

        } catch (Exception e) {
            log.error("Error fetching user points rank for {}: {}", userId, e.getMessage());
            return calculateUserPointsRank(userId);
        }
    }

    /**
     * Get user's cached rank in level leaderboard
     */
    public Long getUserLevelRank(Long userId) {
        try {
            String cacheKey = USER_RANK_KEY + "level:" + userId;
            String cachedRank = (String) redisTemplate.opsForValue().get(cacheKey);

            if (cachedRank != null) {
                return Long.parseLong(cachedRank);
            }

            Long rank = calculateUserLevelRank(userId);
            redisTemplate.opsForValue().set(cacheKey, rank.toString(), USER_RANK_TTL);

            return rank;

        } catch (Exception e) {
            log.error("Error fetching user level rank for {}: {}", userId, e.getMessage());
            return calculateUserLevelRank(userId);
        }
    }

    // ===============================
    // CACHE INVALIDATION METHODS
    // ===============================

    /**
     * Invalidate all leaderboard caches
     */
    @CacheEvict(value = { "pointsLeaderboard", "levelLeaderboard", "streakLeaderboard" }, allEntries = true)
    public void invalidateAllLeaderboards() {
        log.info("Invalidating all leaderboard caches");

        try {
            // Clear Redis caches
            clearRedisPattern(POINTS_LEADERBOARD_KEY + "*");
            clearRedisPattern(LEVEL_LEADERBOARD_KEY + "*");
            clearRedisPattern(STREAK_LEADERBOARD_KEY + "*");
            clearRedisPattern(USER_RANK_KEY + "*");

        } catch (Exception e) {
            log.error("Error invalidating leaderboard caches: {}", e.getMessage());
        }
    }

    /**
     * Invalidate caches for a specific user (when their data changes)
     */
    public void invalidateUserCaches(Long userId) {
        log.debug("Invalidating caches for user {}", userId);

        try {
            // Clear user-specific rank caches
            redisTemplate.delete(USER_RANK_KEY + "points:" + userId);
            redisTemplate.delete(USER_RANK_KEY + "level:" + userId);

            // Could be more selective, but for simplicity, clear leaderboards
            // In production, you might only clear if user is in top rankings
            invalidateAllLeaderboards();

        } catch (Exception e) {
            log.error("Error invalidating caches for user {}: {}", userId, e.getMessage());
        }
    }

    // ===============================
    // CACHE WARMING METHODS
    // ===============================

    /**
     * Warm up leaderboard caches (scheduled every 15 minutes)
     */
    @Scheduled(cron = "${gamification.scheduling.leaderboard-refresh.cron:0 */15 * * * ?}")
    public void warmUpLeaderboardCaches() {
        log.info("Starting leaderboard cache warm-up");

        try {
            // Warm up popular leaderboard sizes
            int[] popularLimits = { 10, 25, 50, 100 };

            for (int limit : popularLimits) {
                warmUpPointsLeaderboard(limit);
                warmUpLevelLeaderboard(limit);
                warmUpStreakLeaderboard("workout", limit);
            }

            log.info("Leaderboard cache warm-up completed");

        } catch (Exception e) {
            log.error("Error during leaderboard cache warm-up: {}", e.getMessage(), e);
        }
    }

    private void warmUpPointsLeaderboard(int limit) {
        try {
            String cacheKey = POINTS_LEADERBOARD_KEY + limit;

            // Only warm up if not already cached
            if (!redisTemplate.hasKey(cacheKey)) {
                List<LeaderboardResponse> leaderboard = fetchPointsLeaderboardFromDatabase(limit);
                redisTemplate.opsForValue().set(cacheKey, leaderboard, LEADERBOARD_TTL);
                log.debug("Warmed up points leaderboard for limit {}", limit);
            }

        } catch (Exception e) {
            log.error("Error warming up points leaderboard for limit {}: {}", limit, e.getMessage());
        }
    }

    private void warmUpLevelLeaderboard(int limit) {
        try {
            String cacheKey = LEVEL_LEADERBOARD_KEY + limit;

            if (!redisTemplate.hasKey(cacheKey)) {
                List<LeaderboardResponse> leaderboard = fetchLevelLeaderboardFromDatabase(limit);
                redisTemplate.opsForValue().set(cacheKey, leaderboard, LEADERBOARD_TTL);
                log.debug("Warmed up level leaderboard for limit {}", limit);
            }

        } catch (Exception e) {
            log.error("Error warming up level leaderboard for limit {}: {}", limit, e.getMessage());
        }
    }

    private void warmUpStreakLeaderboard(String streakType, int limit) {
        try {
            String cacheKey = STREAK_LEADERBOARD_KEY + streakType + ":" + limit;

            if (!redisTemplate.hasKey(cacheKey)) {
                List<LeaderboardResponse> leaderboard = fetchStreakLeaderboardFromDatabase(streakType, limit);
                redisTemplate.opsForValue().set(cacheKey, leaderboard, LEADERBOARD_TTL);
                log.debug("Warmed up {} streak leaderboard for limit {}", streakType, limit);
            }

        } catch (Exception e) {
            log.error("Error warming up {} streak leaderboard for limit {}: {}", streakType, limit, e.getMessage());
        }
    }

    // ===============================
    // DATABASE FETCH METHODS
    // ===============================

    private List<LeaderboardResponse> fetchPointsLeaderboardFromDatabase(int limit) {
        List<UserGamificationProfile> users = userProfileRepository
                .findAllByOrderByPointsDesc(PageRequest.of(0, limit));

        return IntStream.range(0, users.size())
                .mapToObj(i -> createLeaderboardResponse(users.get(i), i + 1, "points"))
                .toList();
    }

    private List<LeaderboardResponse> fetchLevelLeaderboardFromDatabase(int limit) {
        List<UserGamificationProfile> users = userProfileRepository
                .findAllByOrderByLevelDesc(PageRequest.of(0, limit));

        return IntStream.range(0, users.size())
                .mapToObj(i -> createLeaderboardResponse(users.get(i), i + 1, "level"))
                .toList();
    }

    private List<LeaderboardResponse> fetchStreakLeaderboardFromDatabase(String streakType, int limit) {
        List<UserGamificationProfile> users = userProfileRepository
                .findTopUsersByStreak(streakType, PageRequest.of(0, limit));

        return IntStream.range(0, users.size())
                .mapToObj(i -> createLeaderboardResponse(users.get(i), i + 1, "streak"))
                .toList();
    }

    private LeaderboardResponse createLeaderboardResponse(UserGamificationProfile user, int rank, String type) {
        LeaderboardResponse response = new LeaderboardResponse();
        response.setUserId(user.getUserId());
        response.setRank(rank);
        response.setPoints(user.getPoints());
        response.setLevel(user.getLevel());

        // Add streak information if applicable
        if ("streak".equals(type) && user.getStreaks() != null) {
            user.getStreaks().values().stream()
                    .mapToInt(UserGamificationProfile.StreakData::getCurrent)
                    .max()
                    .ifPresent(response::setCurrentStreak);
        }

        return response;
    }

    private Long calculateUserPointsRank(Long userId) {
        try {
            UserGamificationProfile user = userProfileRepository.findByUserId(userId).orElse(null);
            if (user == null)
                return 0L;

            return userProfileRepository.countUsersWithHigherPoints(user.getPoints()) + 1;

        } catch (Exception e) {
            log.error("Error calculating points rank for user {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    private Long calculateUserLevelRank(Long userId) {
        try {
            UserGamificationProfile user = userProfileRepository.findByUserId(userId).orElse(null);
            if (user == null)
                return 0L;

            return userProfileRepository.countUsersWithHigherLevel(user.getLevel()) + 1;

        } catch (Exception e) {
            log.error("Error calculating level rank for user {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    private void clearRedisPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Cleared {} Redis keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error clearing Redis pattern {}: {}", pattern, e.getMessage());
        }
    }

    /**
     * Health check for cache connectivity
     */
    public boolean isCacheHealthy() {
        try {
            redisTemplate.opsForValue().set("health-check", "ok", Duration.ofSeconds(5));
            String result = (String) redisTemplate.opsForValue().get("health-check");
            return "ok".equals(result);
        } catch (Exception e) {
            log.warn("Cache health check failed: {}", e.getMessage());
            return false;
        }
    }
}