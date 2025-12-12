package com.muscledia.Gamification_service.mapper;

import com.muscledia.Gamification_service.dto.response.LeaderboardResponse;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting UserGamificationProfile to LeaderboardResponse
 */
@Component
public class LeaderboardMapper {

    /**
     * Map UserGamificationProfile to LeaderboardResponse with rank
     */
    public LeaderboardResponse toLeaderboardResponse(UserGamificationProfile profile, int rank) {
        LeaderboardResponse response = new LeaderboardResponse();
        response.setUserId(profile.getUserId());
        response.setRank(rank);
        response.setPoints(profile.getPoints());
        response.setLevel(profile.getLevel());
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
        response.setCurrentStreak(profile.getWeeklyStreak());
        response.setLongestStreak(profile.getLongestWeeklyStreak());
        return response;
    }

    /**
     * Map UserGamificationProfile to LeaderboardResponse for monthly streak
     */
    public LeaderboardResponse toMonthlyStreakResponse(UserGamificationProfile profile, int rank) {
        LeaderboardResponse response = toLeaderboardResponse(profile, rank);
        response.setCurrentStreak(profile.getMonthlyStreak());
        response.setLongestStreak(profile.getLongestMonthlyStreak());
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