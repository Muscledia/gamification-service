package com.muscledia.Gamification_service.service.cache;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.service.UserGamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIMPLE MVP CACHE SERVICE - DEFAULT FOR MVP
 * 
 * No Redis, no complexity, just basic in-memory cache
 * Perfect for MVP - handles 1000+ users easily
 * Cost: $0
 * 
 * This is the DEFAULT cache service when Redis is disabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.cost-optimization.redis-enabled", havingValue = "false", matchIfMissing = true)
public class SimpleMVPCacheService {

    private final UserGamificationService userGamificationService;

    // Simple in-memory cache
    private final ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<>();

    static class CacheItem {
        final List<UserGamificationProfile> data;
        final long expiryTime;

        CacheItem(List<UserGamificationProfile> data, long ttlMinutes) {
            this.data = data;
            this.expiryTime = System.currentTimeMillis() + (ttlMinutes * 60 * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    /**
     * Get leaderboard with simple caching
     * First call: ~200ms (database)
     * Subsequent calls: ~1ms (cache hit)
     */
    public List<UserGamificationProfile> getPointsLeaderboard(int limit) {
        String cacheKey = "points_" + limit;

        CacheItem cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache HIT for points leaderboard ({})", limit);
            return cached.data;
        }

        log.debug("Cache MISS for points leaderboard ({})", limit);
        List<UserGamificationProfile> fresh = userGamificationService.getTopUsersByPoints(limit);

        // Cache for 10 minutes
        cache.put(cacheKey, new CacheItem(fresh, 10));

        return fresh;
    }

    /**
     * Get level leaderboard with simple caching
     */
    public List<UserGamificationProfile> getLevelLeaderboard(int limit) {
        String cacheKey = "level_" + limit;

        CacheItem cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache HIT for level leaderboard ({})", limit);
            return cached.data;
        }

        log.debug("Cache MISS for level leaderboard ({})", limit);
        List<UserGamificationProfile> fresh = userGamificationService.getTopUsersByLevel(limit);

        // Cache for 10 minutes
        cache.put(cacheKey, new CacheItem(fresh, 10));

        return fresh;
    }

    /**
     * Clear cache when user data changes (simple invalidation)
     */
    public void clearCache() {
        cache.clear();
        log.info("Cleared simple MVP cache");
    }

    /**
     * Get cache stats for monitoring
     */
    public String getCacheStats() {
        long validEntries = cache.values().stream()
                .mapToLong(item -> item.isExpired() ? 0 : 1)
                .sum();

        return String.format("Cache: %d total, %d valid, %d expired",
                cache.size(), validEntries, cache.size() - validEntries);
    }
}