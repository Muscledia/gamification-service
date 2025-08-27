package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.UserRegisteredEvent;
import com.muscledia.Gamification_service.exception.UserProfileException;
import com.muscledia.Gamification_service.model.UserBadge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class UserGamificationService {

    private final UserGamificationProfileRepository userProfileRepository;
    private final ProfileCreationService profileCreationService;
    private final ProfileUpdateService profileUpdateService;
    private final WelcomeAchievementService welcomeAchievementService;
    private final LeaderboardService leaderboardService;
    private final AnalyticsService analyticsService;

    // ===========================================
    // EVENT PROCESSING METHODS
    // ===========================================

    /**
     * Process user registration event (main entry point for event consumers)
     */
    @Transactional
    public void processUserRegistration(UserRegisteredEvent event) {
        log.info("🎮 Processing user registration for user: {} ({})",
                event.getUserId(), event.getUsername());

        try {
            // Check if profile already exists (idempotency)
            if (profileExists(event.getUserId())) {
                log.info("Profile already exists for user {}, skipping creation", event.getUserId());
                return;
            }

            // Create new gamification profile
            UserGamificationProfile profile = profileCreationService.createProfile(event);

            // Award welcome achievement
            welcomeAchievementService.awardWelcomeAchievement(profile);

            log.info("Successfully created gamification profile for user {}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process user registration for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw new UserProfileException("Failed to create gamification profile", e);
        }
    }

    // ===========================================
    // PROFILE MANAGEMENT METHODS
    // ===========================================

    @Transactional
    public UserGamificationProfile createOrGetUserProfile(Long userId) {
        log.debug("Creating or getting user profile for user {}", userId);

        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> profileCreationService.createDefaultProfile(userId));
    }

    public UserGamificationProfile getUserProfile(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new UserProfileException("User profile not found: " + userId));
    }

    @Transactional
    public UserGamificationProfile saveUserProfile(UserGamificationProfile profile) {
        return profileUpdateService.saveProfile(profile);
    }

    // ===========================================
    // POINTS AND LEVEL MANAGEMENT
    // ===========================================

    @Transactional
    public UserGamificationProfile updateUserPoints(Long userId, int pointsToAdd) {
        log.info("Adding {} points to user {}", pointsToAdd, userId);

        UserGamificationProfile profile = createOrGetUserProfile(userId);
        return profileUpdateService.updatePoints(profile, pointsToAdd);
    }

    @Transactional
    public UserGamificationProfile updateUserStreak(Long userId, String streakType, boolean streakContinues) {
        log.info("Updating {} streak for user {}: continues={}", streakType, userId, streakContinues);

        UserGamificationProfile profile = getUserProfile(userId);
        return profileUpdateService.updateStreak(profile, streakType, streakContinues);
    }

    // ===========================================
    // STREAK METHODS (REQUIRED BY CONTROLLER)
    // ===========================================

    public int getUserCurrentStreak(Long userId, String streakType) {
        return analyticsService.getCurrentStreak(userId, streakType);
    }

    public int getUserLongestStreak(Long userId, String streakType) {
        return analyticsService.getLongestStreak(userId, streakType);
    }

    // ===========================================
    // RANKING METHODS (REQUIRED BY CONTROLLER)
    // ===========================================

    public long getUserPointsRank(Long userId) {
        return leaderboardService.getUserPointsRank(userId);
    }

    public long getUserLevelRank(Long userId) {
        return leaderboardService.getUserLevelRank(userId);
    }

    // ===========================================
    // LEADERBOARD METHODS (REQUIRED BY CONTROLLER)
    // ===========================================

    public List<UserGamificationProfile> getPointsLeaderboard(int limit) {
        log.info("Getting points leaderboard with limit {}", limit);
        return leaderboardService.getPointsLeaderboard(limit);
    }

    public List<UserGamificationProfile> getLevelLeaderboard(int limit) {
        log.info("Getting level leaderboard with limit {}", limit);
        return leaderboardService.getLevelLeaderboard(limit);
    }

    public List<UserGamificationProfile> getStreakLeaderboard(String streakType, int limit) {
        log.info("Getting {} streak leaderboard with limit {}", streakType, limit);
        return leaderboardService.getStreakLeaderboard(streakType, limit);
    }

    public List<UserGamificationProfile> getLongestStreakLeaderboard(String streakType, int limit) {
        log.info("Getting {} longest streak leaderboard with limit {}", streakType, limit);
        return leaderboardService.getLongestStreakLeaderboard(streakType, limit);
    }

    // ===========================================
    // ANALYTICS METHODS (REQUIRED BY CONTROLLER)
    // ===========================================

    public Map<String, Object> getUserAchievementsSummary(Long userId) {
        return analyticsService.getUserAchievementsSummary(userId);
    }

    public Map<String, Object> getPlatformStatistics() {
        return analyticsService.getPlatformStatistics();
    }

    public List<UserGamificationProfile> getRecentLevelUps(int hoursBack) {
        log.info("Getting recent level ups within {} hours", hoursBack);
        return analyticsService.getRecentLevelUps(hoursBack);
    }

    public List<UserGamificationProfile> getUsersWithActiveStreak(String streakType) {
        log.info("Getting users with active {} streak", streakType);
        return analyticsService.getUsersWithActiveStreak(streakType);
    }

    public List<UserGamificationProfile> getUsersWithMinimumStreak(String streakType, int minLength) {
        log.info("Getting users with minimum {} streak of {}", streakType, minLength);
        return analyticsService.getUsersWithMinimumStreak(streakType, minLength);
    }

    // ===========================================
    // ADMIN METHODS
    // ===========================================

    @Transactional
    public UserGamificationProfile resetUserProgress(Long userId) {
        log.warn("Resetting progress for user {}", userId);

        UserGamificationProfile profile = getUserProfile(userId);
        return profileUpdateService.resetProgress(profile);
    }

    @Transactional
    public void deleteUserProfile(Long userId) {
        log.warn("🗑️ Deleting user profile for user {}", userId);

        UserGamificationProfile profile = getUserProfile(userId);
        userProfileRepository.delete(profile);
    }

    // ===========================================
    // PRIVATE HELPER METHODS
    // ===========================================

    private boolean profileExists(Long userId) {
        return userProfileRepository.findByUserId(userId).isPresent();
    }
}