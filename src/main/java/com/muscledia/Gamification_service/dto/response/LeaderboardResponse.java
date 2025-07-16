package com.muscledia.Gamification_service.dto.response;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {

    private String leaderboardType;
    private String streakType; // Only used for streak leaderboards
    private int limit;
    private int totalEntries;
    private List<UserGamificationProfile> users;
}