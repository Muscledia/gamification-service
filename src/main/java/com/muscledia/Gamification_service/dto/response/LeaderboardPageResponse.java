package com.muscledia.Gamification_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Paginated leaderboard response with user context.
 * Provides both the top users and current user's position.
 */
@Data
@Builder
public class LeaderboardPageResponse {

    /**
     * Top users on the leaderboard (for current page)
     */
    private List<LeaderboardResponse> leaderboard;

    /**
     * Current user's position and stats (even if not in top list)
     */
    private LeaderboardResponse currentUser;

    /**
     * Users around current user (5 above, 5 below)
     */
    private List<LeaderboardResponse> nearbyUsers;

    /**
     * Pagination info
     */
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;

    /**
     * Total number of users in the leaderboard
     */
    private Integer totalUsers;

    /**
     * Type of leaderboard (POINTS, LEVEL, WEEKLY_STREAK, MONTHLY_STREAK)
     */
    private String leaderboardType;

    /**
     * Whether current user is in the top list
     */
    private Boolean currentUserInTopList;
}