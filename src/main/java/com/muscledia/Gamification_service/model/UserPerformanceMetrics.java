package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@Builder
public class UserPerformanceMetrics {
    private Long userId;
    private double recentCompletionRate;
    private int currentLevel;
    private int totalPoints;

    // Performance analysis
    private Map<ObjectiveType, Double> objectivePerformance;
    private int averageCompletionTimeHours;
    private int consecutiveCompletions;
    private Set<ChallengeType> preferredChallengeTypes;

    // Additional metrics
    private double consistencyScore;
    private int totalChallengesAttempted;
    private int totalChallengesCompleted;

    // Helper methods
    public boolean isPerformingWell() {
        return recentCompletionRate > 0.7;
    }

    public boolean needsEasierChallenges() {
        return recentCompletionRate < 0.3;
    }
}
