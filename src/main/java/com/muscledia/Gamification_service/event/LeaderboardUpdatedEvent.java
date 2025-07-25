package com.muscledia.Gamification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import java.util.Map;

/**
 * Event published when leaderboard positions change significantly.
 * This is an OUTBOUND event for leaderboard notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeaderboardUpdatedEvent extends BaseEvent {

    @NotBlank
    private String leaderboardType; // "POINTS", "LEVEL", "STREAK"

    @Min(1)
    private Integer newRank;

    private Integer previousRank;

    @Min(0)
    private Integer currentValue; // points, level, or streak value

    private String changeType; // "RANK_UP", "RANK_DOWN", "NEW_ENTRY", "TOP_10_ENTRY"

    private Map<String, Object> leaderboardContext;

    @Override
    public String getEventType() {
        return "LEADERBOARD_UPDATED";
    }

    @Override
    public boolean isValid() {
        return leaderboardType != null && !leaderboardType.trim().isEmpty()
                && newRank != null && newRank >= 1
                && currentValue != null && currentValue >= 0
                && changeType != null && !changeType.trim().isEmpty();
    }

    public boolean isSignificantChange() {
        if (previousRank == null)
            return newRank <= 100; // New entry in top 100

        int rankChange = Math.abs(newRank - previousRank);
        return switch (newRank) {
            case 1, 2, 3 -> true; // Top 3 is always significant
            default -> newRank <= 10 || rankChange >= 10; // Top 10 or big jumps
        };
    }

    public int getRankChange() {
        return previousRank != null ? previousRank - newRank : 0;
    }
}