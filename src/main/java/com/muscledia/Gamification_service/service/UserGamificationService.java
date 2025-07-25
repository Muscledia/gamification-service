package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGamificationService {

    private final UserGamificationProfileRepository userProfileRepository;

    /**
     * Create or get user gamification profile
     */
    @Transactional
    public UserGamificationProfile createOrGetUserProfile(Long userId) {
        log.info("Creating or getting user profile for user {}", userId);

        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserGamificationProfile newProfile = new UserGamificationProfile();
                    newProfile.setUserId(userId);
                    newProfile.setPoints(0);
                    newProfile.setLevel(1);
                    newProfile.setLastLevelUpDate(Instant.now());
                    newProfile.setStreaks(new HashMap<>());
                    newProfile.setEarnedBadges(new ArrayList<>());

                    UserGamificationProfile savedProfile = userProfileRepository.save(newProfile);
                    log.info("Created new user profile for user {}", userId);
                    return savedProfile;
                });
    }

    /**
     * Get user profile by user ID
     */
    public UserGamificationProfile getUserProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));
    }

    /**
     * Update user points and check for level up
     */
    @Transactional
    public UserGamificationProfile updateUserPoints(Long userId, int pointsToAdd) {
        log.info("Adding {} points to user {}", pointsToAdd, userId);

        UserGamificationProfile userProfile = getUserProfile(userId);

        int oldPoints = userProfile.getPoints();
        int newPoints = oldPoints + pointsToAdd;
        userProfile.setPoints(newPoints);

        // Check for level up
        int oldLevel = userProfile.getLevel();
        int newLevel = calculateLevel(newPoints);

        if (newLevel > oldLevel) {
            userProfile.setLevel(newLevel);
            userProfile.setLastLevelUpDate(Instant.now());
            log.info("User {} leveled up from {} to {}", userId, oldLevel, newLevel);
        }

        UserGamificationProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("Updated user {} points: {} -> {}", userId, oldPoints, newPoints);
        return savedProfile;
    }

    /**
     * Update user streak
     */
    @Transactional
    public UserGamificationProfile updateUserStreak(Long userId, String streakType, boolean streakContinues) {
        log.info("Updating {} streak for user {}: continues={}", streakType, userId, streakContinues);

        UserGamificationProfile userProfile = getUserProfile(userId);
        Map<String, UserGamificationProfile.StreakData> streaks = userProfile.getStreaks();

        if (streaks == null) {
            streaks = new HashMap<>();
            userProfile.setStreaks(streaks);
        }

        UserGamificationProfile.StreakData streakData = streaks.get(streakType);
        if (streakData == null) {
            streakData = new UserGamificationProfile.StreakData();
            streakData.setCurrent(0);
            streakData.setLongest(0);
            streaks.put(streakType, streakData);
        }

        Instant now = Instant.now();

        if (streakContinues) {
            // Check if this is a new day/activity for the streak
            if (streakData.getLastUpdate() == null ||
                    ChronoUnit.DAYS.between(streakData.getLastUpdate(), now) >= 1) {

                streakData.setCurrent(streakData.getCurrent() + 1);
                streakData.setLastUpdate(now);

                // Update longest streak if needed
                if (streakData.getCurrent() > streakData.getLongest()) {
                    streakData.setLongest(streakData.getCurrent());
                }

                log.info("User {} {} streak increased to {}", userId, streakType, streakData.getCurrent());
            }
        } else {
            // Streak broken
            if (streakData.getCurrent() > 0) {
                log.info("User {} {} streak broken at {}", userId, streakType, streakData.getCurrent());
                streakData.setCurrent(0);
                streakData.setLastUpdate(now);
            }
        }

        UserGamificationProfile savedProfile = userProfileRepository.save(userProfile);
        return savedProfile;
    }

    /**
     * Get user's current streak for a specific type
     */
    public int getUserCurrentStreak(Long userId, String streakType) {
        UserGamificationProfile userProfile = getUserProfile(userId);

        if (userProfile.getStreaks() == null) {
            return 0;
        }

        UserGamificationProfile.StreakData streakData = userProfile.getStreaks().get(streakType);
        return streakData != null ? streakData.getCurrent() : 0;
    }

    /**
     * Get user's longest streak for a specific type
     */
    public int getUserLongestStreak(Long userId, String streakType) {
        UserGamificationProfile userProfile = getUserProfile(userId);

        if (userProfile.getStreaks() == null) {
            return 0;
        }

        UserGamificationProfile.StreakData streakData = userProfile.getStreaks().get(streakType);
        return streakData != null ? streakData.getLongest() : 0;
    }

    /**
     * Get leaderboard by points
     */
    public List<UserGamificationProfile> getPointsLeaderboard(int limit) {
        log.info("Getting points leaderboard with limit {}", limit);

        Pageable pageable = PageRequest.of(0, limit);
        return userProfileRepository.findAllByOrderByPointsDesc(pageable);
    }

    /**
     * Get leaderboard by level
     */
    public List<UserGamificationProfile> getLevelLeaderboard(int limit) {
        log.info("Getting level leaderboard with limit {}", limit);

        Pageable pageable = PageRequest.of(0, limit);
        return userProfileRepository.findAllByOrderByLevelDesc(pageable);
    }

    /**
     * Get streak leaderboard for a specific streak type
     */
    public List<UserGamificationProfile> getStreakLeaderboard(String streakType, int limit) {
        log.info("Getting {} streak leaderboard with limit {}", streakType, limit);

        Pageable pageable = PageRequest.of(0, limit);
        return userProfileRepository.findTopUsersByStreak(streakType, pageable);
    }

    /**
     * Get longest streak leaderboard for a specific streak type
     */
    public List<UserGamificationProfile> getLongestStreakLeaderboard(String streakType, int limit) {
        log.info("Getting {} longest streak leaderboard with limit {}", streakType, limit);

        Pageable pageable = PageRequest.of(0, limit);
        return userProfileRepository.findTopUsersByLongestStreak(streakType, pageable);
    }

    /**
     * Get leaderboard by points (simple)
     */
    public List<UserGamificationProfile> getTopUsersByPoints(int limit) {
        log.info("Getting top {} users by points", limit);
        return userProfileRepository.findAllByOrderByPointsDesc(PageRequest.of(0, limit));
    }

    /**
     * Get leaderboard by level (simple)
     */
    public List<UserGamificationProfile> getTopUsersByLevel(int limit) {
        log.info("Getting top {} users by level", limit);
        return userProfileRepository.findAllByOrderByLevelDesc(PageRequest.of(0, limit));
    }

    /**
     * Get user's rank by points
     */
    public long getUserPointsRank(Long userId) {
        UserGamificationProfile userProfile = getUserProfile(userId);
        return userProfileRepository.countUsersWithHigherPoints(userProfile.getPoints()) + 1;
    }

    /**
     * Get user's rank by level
     */
    public long getUserLevelRank(Long userId) {
        UserGamificationProfile userProfile = getUserProfile(userId);
        return userProfileRepository.countUsersWithHigherLevel(userProfile.getLevel()) + 1;
    }

    /**
     * Get users who leveled up recently
     */
    public List<UserGamificationProfile> getRecentLevelUps(int hoursBack) {
        Instant since = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        return userProfileRepository.findUsersWithRecentLevelUp(since);
    }

    /**
     * Get users with active streaks
     */
    public List<UserGamificationProfile> getUsersWithActiveStreak(String streakType) {
        return userProfileRepository.findUsersWithActiveStreak(streakType);
    }

    /**
     * Get users with minimum streak length
     */
    public List<UserGamificationProfile> getUsersWithMinimumStreak(String streakType, int minLength) {
        return userProfileRepository.findUsersWithStreakLength(streakType, minLength);
    }

    /**
     * Get platform statistics
     */
    public Map<String, Object> getPlatformStatistics() {
        log.info("Getting platform statistics");

        Map<String, Object> stats = new HashMap<>();

        // Total users
        long totalUsers = userProfileRepository.count();
        stats.put("totalUsers", totalUsers);

        // Level distribution
        Map<Integer, Long> levelDistribution = new HashMap<>();
        for (int level = 1; level <= 10; level++) {
            levelDistribution.put(level, userProfileRepository.countByLevel(level));
        }
        stats.put("levelDistribution", levelDistribution);

        // Points distribution
        Map<String, Long> pointsDistribution = new HashMap<>();
        pointsDistribution.put("0-100", userProfileRepository.countByLevel(1));
        pointsDistribution.put("100-300", userProfileRepository.countByLevel(2));
        pointsDistribution.put("300-600", userProfileRepository.countByLevel(3));
        pointsDistribution.put("600-1000", userProfileRepository.countByLevel(4));
        pointsDistribution.put("1000+", userProfileRepository.countByPointsGreaterThanEqual(1000));
        stats.put("pointsDistribution", pointsDistribution);

        // Recent activity
        List<UserGamificationProfile> recentLevelUps = getRecentLevelUps(24);
        stats.put("recentLevelUps", recentLevelUps.size());

        return stats;
    }

    /**
     * Get user achievements summary
     */
    public Map<String, Object> getUserAchievementsSummary(Long userId) {
        log.info("Getting achievements summary for user {}", userId);

        UserGamificationProfile userProfile = getUserProfile(userId);
        Map<String, Object> summary = new HashMap<>();

        // Basic stats
        summary.put("userId", userId);
        summary.put("level", userProfile.getLevel());
        summary.put("points", userProfile.getPoints());
        summary.put("lastLevelUpDate", userProfile.getLastLevelUpDate());

        // Badge count
        summary.put("totalBadges", userProfile.getEarnedBadges().size());

        // Streak information
        Map<String, Object> streakInfo = new HashMap<>();
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

        // Rankings
        summary.put("pointsRank", getUserPointsRank(userId));
        summary.put("levelRank", getUserLevelRank(userId));

        return summary;
    }

    /**
     * Reset user progress (admin function)
     */
    @Transactional
    public UserGamificationProfile resetUserProgress(Long userId) {
        log.warn("Resetting progress for user {}", userId);

        UserGamificationProfile userProfile = getUserProfile(userId);

        userProfile.setPoints(0);
        userProfile.setLevel(1);
        userProfile.setLastLevelUpDate(Instant.now());
        userProfile.setStreaks(new HashMap<>());
        userProfile.setEarnedBadges(new ArrayList<>());

        UserGamificationProfile savedProfile = userProfileRepository.save(userProfile);
        log.warn("Reset progress for user {}", userId);
        return savedProfile;
    }

    /**
     * Delete user profile
     */
    @Transactional
    public void deleteUserProfile(Long userId) {
        log.info("Deleting user profile for user {}", userId);

        UserGamificationProfile userProfile = getUserProfile(userId);
        userProfileRepository.delete(userProfile);

        log.info("Deleted user profile for user {}", userId);
    }

    /**
     * Private helper method to calculate level from points
     */
    private int calculateLevel(int points) {
        // Progressive level calculation
        if (points < 100)
            return 1;
        if (points < 300)
            return 2;
        if (points < 600)
            return 3;
        if (points < 1000)
            return 4;
        if (points < 1500)
            return 5;
        if (points < 2100)
            return 6;
        if (points < 2800)
            return 7;
        if (points < 3600)
            return 8;
        if (points < 4500)
            return 9;
        return 10 + (points - 4500) / 1000; // Level 10+ requires 1000 points each
    }
}