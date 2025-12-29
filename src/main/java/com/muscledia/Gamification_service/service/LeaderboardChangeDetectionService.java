package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.LeaderboardUpdatedEvent;
import com.muscledia.Gamification_service.event.publisher.EventPublisher;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * PURPOSE: Detect and publish significant leaderboard rank changes
 * RESPONSIBILITY: Monitor rank changes and publish events
 * COUPLING: Low - uses repositories and event publisher
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardChangeDetectionService {

    private final UserGamificationProfileRepository profileRepository;
    private final EventPublisher eventPublisher;

    /**
     * Check and publish points leaderboard changes
     */
    @Transactional
    public void checkPointsRankChange(Long userId, int oldPoints, int newPoints) {
        if (oldPoints == newPoints) return;

        UserGamificationProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return;

        // Calculate old rank (approximate based on old points)
        long oldRank = profileRepository.countUsersWithHigherPoints(oldPoints) + 1;

        // Calculate new rank
        long newRank = profileRepository.countUsersWithHigherPoints(newPoints) + 1;

        if (isSignificantChange(oldRank, newRank)) {
            publishRankChange(userId, "POINTS", (int) oldRank, (int) newRank, newPoints);
        }
    }

    /**
     * Check and publish level leaderboard changes
     */
    @Transactional
    public void checkLevelRankChange(Long userId, int oldLevel, int newLevel) {
        if (oldLevel == newLevel) return;

        UserGamificationProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return;

        long oldRank = profileRepository.countUsersWithHigherLevel(oldLevel) + 1;
        long newRank = profileRepository.countUsersWithHigherLevel(newLevel) + 1;

        if (isSignificantChange(oldRank, newRank)) {
            publishRankChange(userId, "LEVEL", (int) oldRank, (int) newRank, newLevel);
        }
    }

    /**
     * Check and publish weekly streak leaderboard changes
     */
    @Transactional
    public void checkWeeklyStreakRankChange(Long userId, int oldStreak, int newStreak) {
        if (oldStreak == newStreak) return;

        long usersAboveOld = profileRepository.findByWeeklyStreakGreaterThanEqual(oldStreak + 1).size();
        long usersAboveNew = profileRepository.findByWeeklyStreakGreaterThanEqual(newStreak + 1).size();

        long oldRank = usersAboveOld + 1;
        long newRank = usersAboveNew + 1;

        if (isSignificantChange(oldRank, newRank)) {
            publishRankChange(userId, "WEEKLY_STREAK", (int) oldRank, (int) newRank, newStreak);
        }
    }

    /**
     * Check and publish monthly streak leaderboard changes
     */
    @Transactional
    public void checkMonthlyStreakRankChange(Long userId, int oldStreak, int newStreak) {
        if (oldStreak == newStreak) return;

        long usersAboveOld = profileRepository.findByMonthlyStreakGreaterThanEqual(oldStreak + 1).size();
        long usersAboveNew = profileRepository.findByMonthlyStreakGreaterThanEqual(newStreak + 1).size();

        long oldRank = usersAboveOld + 1;
        long newRank = usersAboveNew + 1;

        if (isSignificantChange(oldRank, newRank)) {
            publishRankChange(userId, "MONTHLY_STREAK", (int) oldRank, (int) newRank, newStreak);
        }
    }

    /**
     * Determine if rank change is significant enough to publish
     */
    private boolean isSignificantChange(long oldRank, long newRank) {
        if (oldRank == newRank) return false;

        // Always notify for top 10
        if (newRank <= 10) return true;

        // Notify for top 100 entries
        if (oldRank > 100 && newRank <= 100) return true;

        // Notify for rank changes of 10+ positions
        long rankChange = Math.abs(newRank - oldRank);
        return rankChange >= 10;
    }

    /**
     * Publish leaderboard rank change event
     */
    private void publishRankChange(Long userId, String type, int oldRank, int newRank, int value) {
        String changeType = determineChangeType(oldRank, newRank);

        Map<String, Object> context = new HashMap<>();
        context.put("rankChange", oldRank - newRank);
        context.put("timestamp", Instant.now().toString());

        LeaderboardUpdatedEvent event = LeaderboardUpdatedEvent.builder()
                .userId(userId)
                .leaderboardType(type)
                .previousRank(oldRank)
                .newRank(newRank)
                .currentValue(value)
                .changeType(changeType)
                .leaderboardContext(context)
                .timestamp(Instant.now())
                .build();

        eventPublisher.publishLeaderboardUpdated(event);

        log.info("Published leaderboard update: User {} {} from rank {} to rank {} ({})",
                userId, changeType, oldRank, newRank, type);
    }

    /**
     * Determine type of change
     */
    private String determineChangeType(int oldRank, int newRank) {
        if (oldRank > 100 && newRank <= 100) return "TOP_100_ENTRY";
        if (oldRank > 10 && newRank <= 10) return "TOP_10_ENTRY";
        if (newRank < oldRank) return "RANK_UP";
        return "RANK_DOWN";
    }
}