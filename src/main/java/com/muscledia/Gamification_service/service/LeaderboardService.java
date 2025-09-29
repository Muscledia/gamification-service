package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for handling leaderboard operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final UserGamificationProfileRepository profileRepository;

    public List<UserGamificationProfile> getPointsLeaderboard(int limit) {
        log.debug("Getting points leaderboard with limit {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        return profileRepository.findAllByOrderByPointsDesc(pageable);
    }

    public List<UserGamificationProfile> getLevelLeaderboard(int limit) {
        log.debug("Getting level leaderboard with limit {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        return profileRepository.findAllByOrderByLevelDesc(pageable);
    }

    public List<UserGamificationProfile> getStreakLeaderboard(String streakType, int limit) {
        log.debug("Getting {} streak leaderboard with limit {}", streakType, limit);
        Pageable pageable = PageRequest.of(0, limit);
        return profileRepository.findTopUsersByStreak(streakType, pageable);
    }

    public List<UserGamificationProfile> getLongestStreakLeaderboard(String streakType, int limit) {
        log.debug("Getting {} longest streak leaderboard with limit {}", streakType, limit);
        Pageable pageable = PageRequest.of(0, limit);
        return profileRepository.findTopUsersByLongestStreak(streakType, pageable);
    }

    public long getUserPointsRank(Long userId) {
        try {
            UserGamificationProfile userProfile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));
            return profileRepository.countUsersWithHigherPoints(userProfile.getPoints()) + 1;
        } catch (Exception e) {
            log.error("Error getting points rank for user {}: {}", userId, e.getMessage());
            return -1;
        }
    }

    public long getUserLevelRank(Long userId) {
        try {
            UserGamificationProfile userProfile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));
            return profileRepository.countUsersWithHigherLevel(userProfile.getLevel()) + 1;
        } catch (Exception e) {
            log.error("Error getting level rank for user {}: {}", userId, e.getMessage());
            return -1;
        }
    }

    public List<UserGamificationProfile> getTopUsersByPoints(int limit) {
        return getPointsLeaderboard(limit);
    }

    public List<UserGamificationProfile> getTopUsersByLevel(int limit) {
        return getLevelLeaderboard(limit);
    }
}
