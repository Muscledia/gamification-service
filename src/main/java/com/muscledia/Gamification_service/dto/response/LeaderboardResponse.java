package com.muscledia.Gamification_service.dto.response;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for leaderboard entries.
 * Contains user ranking information and relevant stats.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardResponse {

    private Long userId;
    private Integer rank;
    private Integer points;
    private Integer level;
    private Integer currentStreak;
    private String username; // Optional, if available
    private String displayName; // Optional, if available

    // Additional leaderboard-specific fields
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