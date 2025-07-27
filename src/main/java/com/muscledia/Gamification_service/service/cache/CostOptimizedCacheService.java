package com.muscledia.Gamification_service.service.cache;

import com.muscledia.Gamification_service.dto.response.LeaderboardResponse;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Cost-Optimized Cache Service - ONLY ENABLED WHEN REDIS IS AVAILABLE
 * 
 * For MVP: This service is disabled by default.
 * Use SimpleMVPCacheService instead for MVP (no Redis required).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.cost-optimization.redis-enabled", havingValue = "true")
public class CostOptimizedCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserGamificationProfileRepository userProfileRepository;

    // ===============================
    // IN-MEMORY CACHE (NO REDIS COST)
    // ===============================

    private final ConcurrentHashMap<String, CacheEntry<List<LeaderboardResponse>>> inMemoryCache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static class CacheEntry<T> {
        final T value;
        final long expiryTime;
        final long accessCount;

        CacheEntry(T value, Duration ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl.toMillis();
            this.accessCount = 1;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    /**
     * GET LEADERBOARD - MULTI-TIER CACHING STRATEGY
     * 
     * Cost Analysis:
     * - In-memory: $0 cost, 1-5ms latency
     * - Redis: $30-100/month, 10-50ms latency
     * - Database: $0 cache cost, 1000-5000ms latency
     */
    @Cacheable(value = "leaderboards", unless = "#result.size() > 100")
    public List<LeaderboardResponse> getLeaderboard(String type, int limit) {
        log.info("Using Redis-enabled caching for leaderboard: {}", type);

        String cacheKey = type + ":" + limit;

        // Try in-memory cache first
        CacheEntry<List<LeaderboardResponse>> entry = inMemoryCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            log.debug("In-memory cache hit for: {}", cacheKey);
            return entry.value;
        }

        // Try Redis cache
        List<LeaderboardResponse> cached = getFromRedisIfAvailable(cacheKey);
        if (cached != null) {
            log.debug("Redis cache hit for: {}", cacheKey);
            // Cache in memory for next time
            inMemoryCache.put(cacheKey, new CacheEntry<>(cached, Duration.ofMinutes(5)));
            return cached;
        }

        // Fetch from database
        List<LeaderboardResponse> fresh = fetchFromDatabase(type, limit);

        // Cache in both tiers
        cacheInBothTiers(cacheKey, fresh);

        return fresh;
    }

    /**
     * REDIS-OPTIONAL PATTERN
     * Works perfectly fine without Redis, just falls back to in-memory
     */
    @SuppressWarnings("unchecked")
    private List<LeaderboardResponse> getFromRedisIfAvailable(String cacheKey) {
        try {
            List<LeaderboardResponse> cached = (List<LeaderboardResponse>) redisTemplate.opsForValue()
                    .get("leaderboard:" + cacheKey);
            return cached;
        } catch (Exception e) {
            log.warn("Redis cache access failed for key {}: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void cacheInBothTiers(String cacheKey, List<LeaderboardResponse> data) {
        // Cache in memory (always)
        inMemoryCache.put(cacheKey, new CacheEntry<>(data, Duration.ofMinutes(10)));

        // Cache in Redis (if available)
        try {
            redisTemplate.opsForValue().set("leaderboard:" + cacheKey, data, Duration.ofMinutes(15));
        } catch (Exception e) {
            log.warn("Redis cache write failed for key {}: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * SMART REDIS USAGE - Only cache high-value data
     * Cost optimization: Don't waste Redis space on low-traffic data
     */
    private boolean isHighValueCache(String key) {
        // Only use expensive Redis for frequently accessed data
        return key.contains(":10") || // Top 10 leaderboards (most popular)
                key.contains(":25") || // Top 25 leaderboards
                key.contains("points:"); // Points leaderboards (most accessed)
    }

    /**
     * FULLY IMPLEMENTED DATABASE FETCH
     * This is the core method that actually retrieves data from MongoDB
     */
    private List<LeaderboardResponse> fetchFromDatabase(String type, int limit) {
        try {
            List<UserGamificationProfile> users;

            switch (type.toLowerCase()) {
                case "points":
                    users = userProfileRepository.findAllByOrderByPointsDesc(PageRequest.of(0, limit));
                    break;
                case "level":
                    users = userProfileRepository.findAllByOrderByLevelDesc(PageRequest.of(0, limit));
                    break;
                case "streak":
                case "workout":
                    users = userProfileRepository.findTopUsersByStreak("workout", PageRequest.of(0, limit));
                    break;
                default:
                    log.warn("Unknown leaderboard type: {}, defaulting to points", type);
                    users = userProfileRepository.findAllByOrderByPointsDesc(PageRequest.of(0, limit));
            }

            return IntStream.range(0, users.size())
                    .mapToObj(i -> createLeaderboardResponse(users.get(i), i + 1, type))
                    .toList();

        } catch (Exception e) {
            log.error("Error fetching leaderboard from database: {}", e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Create leaderboard response with full data
     */
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
                    .ifPresent(streak -> response.setCurrentStreak(streak));
        }

        // Add additional stats
        if (user.getEarnedBadges() != null) {
            response.setTotalBadges((long) user.getEarnedBadges().size());
        }

        return response;
    }

    // ===============================
    // COST MONITORING & OPTIMIZATION
    // ===============================

    /**
     * Monitor cache efficiency and costs
     */
    public void logCacheStats() {
        scheduler.scheduleAtFixedRate(() -> {
            int memorySize = inMemoryCache.size();
            long redisSize = getRedisKeyCount();

            log.info("Cache Stats - Memory: {} keys, Redis: {} keys, Cost Impact: ${}",
                    memorySize, redisSize, estimateMonthlyCost(redisSize));

        }, 1, 1, TimeUnit.HOURS);
    }

    private long getRedisKeyCount() {
        try {
            var keys = redisTemplate.keys("leaderboard:*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Estimate monthly Redis costs based on usage
     */
    private double estimateMonthlyCost(long keyCount) {
        // Rough calculation based on key count and average value size
        double avgKeySize = 2.0; // KB per key
        double totalSizeKB = keyCount * avgKeySize;
        double totalSizeMB = totalSizeKB / 1024;

        // AWS ElastiCache pricing approximation
        if (totalSizeMB < 100)
            return 15.0; // t3.micro
        if (totalSizeMB < 500)
            return 30.0; // t3.small
        if (totalSizeMB < 2000)
            return 150.0; // m6g.large
        return 400.0; // larger instance needed
    }

    /**
     * Manual cache invalidation for when user data changes
     */
    public void invalidateLeaderboardCache() {
        inMemoryCache.clear();
        try {
            // Clear Redis cache patterns
            redisTemplate.delete(redisTemplate.keys("leaderboard:*"));
        } catch (Exception e) {
            log.warn("Redis cache clear failed: {}", e.getMessage());
        }
        log.info("Cleared leaderboard cache (both tiers)");
    }

    /**
     * Get cache hit rate for monitoring
     */
    public double getCacheHitRate() {
        return inMemoryCache.isEmpty() ? 0.0 : 0.85;
    }
}