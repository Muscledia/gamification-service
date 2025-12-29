package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.LevelUpEvent;
import com.muscledia.Gamification_service.event.publisher.EventPublisher;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import com.muscledia.Gamification_service.service.profile.LevelCalculator;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Specialized service for updating user profiles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileUpdateService {

    private final UserGamificationProfileRepository profileRepository;
    private final EventPublisher eventPublisher;
    private final LeaderboardChangeDetectionService leaderboardDetection;

    public UserGamificationProfile saveProfile(UserGamificationProfile profile) {
        profile.setLastUpdated(Instant.now());
        return profileRepository.save(profile);
    }

    @Transactional
    public UserGamificationProfile updatePoints(UserGamificationProfile profile, int pointsToAdd) {
        int oldPoints = profile.getPoints();
        int oldLevel = profile.getLevel();

        profile.addPoints(pointsToAdd);

        boolean leveledUp = profile.getLevel() > oldLevel;

        if (leveledUp) {
            publishLevelUpEvent(profile, oldLevel);
            // Check level leaderboard change
            leaderboardDetection.checkLevelRankChange(
                    profile.getUserId(), oldLevel, profile.getLevel());
        }

        UserGamificationProfile saved = profileRepository.save(profile);

        // Check points leaderboard change
        leaderboardDetection.checkPointsRankChange(
                profile.getUserId(), oldPoints, profile.getPoints());

        return saved;
    }

    /**
     * Update user streak
     */
    @Transactional
    public UserGamificationProfile updateStreak(UserGamificationProfile profile, String streakType, boolean streakContinues) {
        Map<String, UserGamificationProfile.StreakData> streaks = profile.getStreaks();
        if (streaks == null) {
            streaks = new HashMap<>();
            profile.setStreaks(streaks);
        }

        UserGamificationProfile.StreakData streakData = streaks.computeIfAbsent(streakType,
                k -> new UserGamificationProfile.StreakData());

        Instant now = Instant.now();

        if (streakContinues) {
            updateContinuingStreak(streakData, now, profile.getUserId(), streakType);
        } else {
            breakStreak(streakData, now, profile.getUserId(), streakType);
        }

        return saveProfile(profile);
    }

    public UserGamificationProfile resetProgress(UserGamificationProfile profile) {
        profile.setPoints(0);
        profile.setLevel(1);
        profile.setLastLevelUpDate(Instant.now());
        profile.setStreaks(new HashMap<>());
        profile.setEarnedBadges(new ArrayList<>());

        return saveProfile(profile);
    }

    private void updateContinuingStreak(UserGamificationProfile.StreakData streakData, Instant now, Long userId, String streakType) {
        if (streakData.getLastUpdate() == null ||
                ChronoUnit.DAYS.between(streakData.getLastUpdate(), now) >= 1) {

            streakData.setCurrent(streakData.getCurrent() + 1);
            streakData.setLastUpdate(now);

            if (streakData.getCurrent() > streakData.getLongest()) {
                streakData.setLongest(streakData.getCurrent());
            }

            log.info("User {} {} streak increased to {}", userId, streakType, streakData.getCurrent());
        }
    }

    private void breakStreak(UserGamificationProfile.StreakData streakData, Instant now, Long userId, String streakType) {
        if (streakData.getCurrent() > 0) {
            log.info("User {} {} streak broken at {}", userId, streakType, streakData.getCurrent());
            streakData.setCurrent(0);
            streakData.setLastUpdate(now);
        }
    }

    /**
     * Publish level up event
     */
    private void publishLevelUpEvent(UserGamificationProfile profile, int oldLevel) {
        try {
            LevelUpEvent event = LevelUpEvent.builder()
                    .userId(profile.getUserId())
                    .previousLevel(oldLevel)  // ✅ Changed from oldLevel
                    .newLevel(profile.getLevel())
                    .totalPoints(profile.getPoints())  // ✅ Changed from currentPoints
                    .pointsToNextLevel(calculatePointsToNextLevel(profile.getLevel()))
                    .levelUpAt(Instant.now())
                    .triggeringActivity("POINTS_EARNED")
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishLevelUp(event);

            log.info("🎉 User {} leveled up: {} → {}",
                    profile.getUserId(), oldLevel, profile.getLevel());

        } catch (Exception e) {
            log.error("Failed to publish level up event for user {}: {}",
                    profile.getUserId(), e.getMessage());
        }
    }

    /**
     * Calculate points needed for next level
     */
    private int calculatePointsToNextLevel(int currentLevel) {
        // Based on formula: level = floor(sqrt(points / 100)) + 1
        // Reverse: points = ((level - 1)^2) * 100
        int pointsForNextLevel = (int) Math.pow(currentLevel, 2) * 100;
        return pointsForNextLevel;
    }
}
