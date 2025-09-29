package com.muscledia.Gamification_service.service;


import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Service for analytics and statistics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final UserGamificationProfileRepository profileRepository;

    public int getCurrentStreak(Long userId, String streakType) {
        try {
            UserGamificationProfile userProfile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            if (userProfile.getStreaks() == null) {
                return 0;
            }

            UserGamificationProfile.StreakData streakData = userProfile.getStreaks().get(streakType);
            return streakData != null ? streakData.getCurrent() : 0;
        } catch (Exception e) {
            log.warn("Error getting current streak for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    public int getLongestStreak(Long userId, String streakType) {
        try {
            UserGamificationProfile userProfile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            if (userProfile.getStreaks() == null) {
                return 0;
            }

            UserGamificationProfile.StreakData streakData = userProfile.getStreaks().get(streakType);
            return streakData != null ? streakData.getLongest() : 0;
        } catch (Exception e) {
            log.warn("Error getting longest streak for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    public Map<String, Object> getUserAchievementsSummary(Long userId) {
        log.info("Getting achievements summary for user {}", userId);

        try {
            UserGamificationProfile userProfile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            Map<String, Object> summary = new HashMap<>();

            // Basic stats
            summary.put("userId", userId);
            summary.put("level", userProfile.getLevel());
            summary.put("points", userProfile.getPoints());
            summary.put("lastLevelUpDate", userProfile.getLastLevelUpDate());
            summary.put("profileCreatedAt", userProfile.getProfileCreatedAt());

            // Badge count
            summary.put("totalBadges", userProfile.getEarnedBadges() != null ?
                    userProfile.getEarnedBadges().size() : 0);

            // Streak information
            Map<String, Map<String, Object>> streakInfo = new HashMap<>();
            if (userProfile.getStreaks() != null) {
                userProfile.getStreaks().forEach((streakType, streakData) -> {
                    Map<String, Object> streakDetails = new HashMap<>();
                    streakDetails.put("current", streakData.getCurrent());
                    streakDetails.put("longest", streakData.getLongest());
                    streakDetails.put("lastUpdate", streakData.getLastUpdate());
                    streakInfo.put(streakType, streakDetails);
                });
            }
            summary.put("streaks", streakInfo);

            // Workout stats
            summary.put("totalWorkouts", userProfile.getTotalWorkoutsCompleted());

            return summary;
        } catch (Exception e) {
            log.error("Error getting achievements summary for user {}: {}", userId, e.getMessage());
            return new HashMap<>();
        }
    }

    public Map<String, Object> getPlatformStatistics() {
        log.info("Getting platform statistics");

        Map<String, Object> stats = new HashMap<>();

        try {
            // Total users
            long totalUsers = profileRepository.count();
            stats.put("totalUsers", totalUsers);

            // Level distribution
            Map<String, Long> levelDistribution = new HashMap<>();
            for (int level = 1; level <= 10; level++) {
                levelDistribution.put("level" + level, profileRepository.countByLevel(level));
            }
            stats.put("levelDistribution", levelDistribution);

            // Points distribution
            Map<String, Long> pointsDistribution = new HashMap<>();
            pointsDistribution.put("0-100", profileRepository.countByLevel(1));
            pointsDistribution.put("100-300", profileRepository.countByLevel(2));
            pointsDistribution.put("300-600", profileRepository.countByLevel(3));
            pointsDistribution.put("600-1000", profileRepository.countByLevel(4));
            pointsDistribution.put("1000+", profileRepository.countByPointsGreaterThanEqual(1000));
            stats.put("pointsDistribution", pointsDistribution);

            // Recent activity
            List<UserGamificationProfile> recentLevelUps = getRecentLevelUps(24);
            stats.put("recentLevelUps", recentLevelUps.size());

            // Active users (users with recent activity)
            List<UserGamificationProfile> activeUsers = getUsersWithActiveStreak("workout");
            stats.put("activeUsers", activeUsers.size());

        } catch (Exception e) {
            log.error("Error getting platform statistics: {}", e.getMessage());
        }

        return stats;
    }

    public List<UserGamificationProfile> getRecentLevelUps(int hoursBack) {
        Instant since = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        return profileRepository.findUsersWithRecentLevelUp(since);
    }

    public List<UserGamificationProfile> getUsersWithActiveStreak(String streakType) {
        return profileRepository.findUsersWithActiveStreak(streakType);
    }

    public List<UserGamificationProfile> getUsersWithMinimumStreak(String streakType, int minLength) {
        return profileRepository.findUsersWithStreakLength(streakType, minLength);
    }
}
