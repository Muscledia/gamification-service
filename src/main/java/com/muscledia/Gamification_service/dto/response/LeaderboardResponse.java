package com.muscledia.Gamification_service.dto.response;

import lombok.Data;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Response DTO for leaderboard entries
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardResponse {

    private Long userId;
    private Integer rank;
    private Integer points;
    private Integer level;
    private Integer currentStreak;
    private String username;
    private String displayName;
    private Long totalWorkouts;
    private Long totalBadges;
    private Integer longestStreak;

    public LeaderboardResponse() {
    }

    public LeaderboardResponse(Long userId, Integer rank, Integer points, Integer level) {
        this.userId = userId;
        this.rank = rank;
        this.points = points;
        this.level = level;
    }
}
