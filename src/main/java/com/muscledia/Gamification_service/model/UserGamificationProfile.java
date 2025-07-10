package com.muscledia.Gamification_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "user_gamification_profiles")
public class UserGamificationProfile {
    @Id
    private Long UserId;

    private int points;

    private int level;

    private Instant lastLevelUpDate;

    private Map<String, StreakData> streaks;

    private List<UserBadge> earnedBadges = new ArrayList<>();


    @Data
    public static class StreakData {
        private int current;
        private Instant lastUpdate;
        private int longest; // Optional: track longest streak
    }
}
