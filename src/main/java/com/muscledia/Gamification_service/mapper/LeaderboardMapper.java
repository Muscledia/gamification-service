package com.muscledia.Gamification_service.mapper;

import com.muscledia.Gamification_service.dto.response.LeaderboardResponse;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.service.NameGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting UserGamificationProfile to LeaderboardResponse
 */
@Component
@RequiredArgsConstructor
public class LeaderboardMapper {
    private final NameGeneratorService nameGenerator;

    /**
     * Map UserGamificationProfile to LeaderboardResponse with rank
     */
    public LeaderboardResponse toLeaderboardResponse(UserGamificationProfile profile, int rank) {
        LeaderboardResponse response = new LeaderboardResponse();
        response.setUserId(profile.getUserId());

        // ⬅️ ENHANCED USERNAME HANDLING
        String username = profile.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = nameGenerator.generateUsername();
        }
        response.setUsername(username);

        // ⬅️ ENHANCED DISPLAY NAME HANDLING
        String displayName = profile.getUsername();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = nameGenerator.generateDisplayNameFromUsername(username);
        }
        response.setDisplayName(displayName);

        response.setRank(rank);
        response.setPoints(profile.getPoints() != null ? profile.getPoints() : 0);
        response.setLevel(profile.getLevel() != null ? profile.getLevel() : 1);
        response.setTotalWorkouts(profile.getTotalWorkoutsCompleted() != null
                ? profile.getTotalWorkoutsCompleted().longValue()
                : 0L);
        response.setTotalBadges(profile.getEarnedBadges() != null
                ? (long) profile.getEarnedBadges().size()
                : 0L);

        return response;
    }

    /**
     * Map UserGamificationProfile to LeaderboardResponse for weekly streak
     */
    public LeaderboardResponse toWeeklyStreakResponse(UserGamificationProfile profile, int rank) {
        LeaderboardResponse response = toLeaderboardResponse(profile, rank);
        response.setCurrentStreak(profile.getWeeklyStreak() != null ? profile.getWeeklyStreak() : 0);
        response.setLongestStreak(profile.getLongestWeeklyStreak() != null ? profile.getLongestWeeklyStreak() : 0);
        return response;
    }

    /**
     * Map UserGamificationProfile to LeaderboardResponse for monthly streak
     */
    public LeaderboardResponse toMonthlyStreakResponse(UserGamificationProfile profile, int rank) {
        LeaderboardResponse response = toLeaderboardResponse(profile, rank);
        response.setCurrentStreak(profile.getMonthlyStreak() != null ? profile.getMonthlyStreak() : 0);
        response.setLongestStreak(profile.getLongestMonthlyStreak() != null ? profile.getLongestMonthlyStreak() : 0);
        return response;
    }

    /**
     * Map list of profiles to list of responses with automatic ranking
     */
    public List<LeaderboardResponse> toLeaderboardResponseList(
            List<UserGamificationProfile> profiles) {

        return profiles.stream()
                .map(profile -> {
                    int rank = profiles.indexOf(profile) + 1;
                    return toLeaderboardResponse(profile, rank);
                })
                .collect(Collectors.toList());
    }

    /**
     * Map list of profiles to weekly streak responses
     */
    public List<LeaderboardResponse> toWeeklyStreakResponseList(
            List<UserGamificationProfile> profiles) {

        return profiles.stream()
                .map(profile -> {
                    int rank = profiles.indexOf(profile) + 1;
                    return toWeeklyStreakResponse(profile, rank);
                })
                .collect(Collectors.toList());
    }

    /**
     * Map list of profiles to monthly streak responses
     */
    public List<LeaderboardResponse> toMonthlyStreakResponseList(
            List<UserGamificationProfile> profiles) {

        return profiles.stream()
                .map(profile -> {
                    int rank = profiles.indexOf(profile) + 1;
                    return toMonthlyStreakResponse(profile, rank);
                })
                .collect(Collectors.toList());
    }
}