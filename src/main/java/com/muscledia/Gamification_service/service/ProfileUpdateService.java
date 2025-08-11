package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import com.muscledia.Gamification_service.service.profile.LevelCalculator;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public UserGamificationProfile saveProfile(UserGamificationProfile profile) {
        profile.setLastUpdated(Instant.now());
        return profileRepository.save(profile);
    }

    public UserGamificationProfile updatePoints(UserGamificationProfile profile, int pointsToAdd) {
        int oldPoints = profile.getPoints();
        int newPoints = oldPoints + pointsToAdd;
        profile.setPoints(newPoints);

        // Check for level up
        int oldLevel = profile.getLevel();
        int newLevel = LevelCalculator.calculateLevel(newPoints);

        if (newLevel > oldLevel) {
            profile.setLevel(newLevel);
            profile.setLastLevelUpDate(Instant.now());
            log.info("User {} leveled up from {} to {}!", profile.getUserId(), oldLevel, newLevel);
        }

        return saveProfile(profile);
    }

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
}
