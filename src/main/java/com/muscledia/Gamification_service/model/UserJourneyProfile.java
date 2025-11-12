package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.*;

@Document(collection = "user_journeys")
@Data
@Builder
public class UserJourneyProfile {

    @Id
    private String id;
    private Long userId;

    // Journey Tracking
    private Set<String> activeJourneyTags; // "strength", "endurance", "weight_loss"
    private String currentPhase; // "foundation", "building", "intermediate"
    private int currentLevel;

    // Progress Tracking
    private Set<String> completedChallengeTemplates;
    private Map<String, Integer> templateCompletionCount;
    private Map<String, Double> performanceMetrics;

    // Preferences (learned from user behavior)
    private Set<ObjectiveType> preferredObjectives;
    private DifficultyLevel preferredDifficulty;
    private Set<ChallengeType> preferredTypes;

    // Analytics
    private double averageCompletionRate;
    private int consecutiveChallengesCompleted;
    private Instant lastChallengeCompletedAt;

    // Journey Progression
    private String nextRecommendedPhase;
    private List<String> upcomingMilestones;

    public void updateCompletionRate(DifficultyLevel difficulty) {
        String key = difficulty.name() + "_completion_rate";
        double currentRate = performanceMetrics.getOrDefault(key, 0.0);

        // Simple running average update
        int completions = templateCompletionCount.values().stream()
                .mapToInt(Integer::intValue).sum();

        double newRate = (currentRate * (completions - 1) + 1.0) / completions;
        performanceMetrics.put(key, newRate);

        // Update overall average
        this.averageCompletionRate = performanceMetrics.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public void addPreferredObjective(ObjectiveType objectiveType) {
        if (preferredObjectives == null) {
            preferredObjectives = new HashSet<>();
        }
        preferredObjectives.add(objectiveType);
    }

    public void addJourneyTag(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return;

        if (activeJourneyTags == null) {
            activeJourneyTags = new HashSet<>();
        }
        activeJourneyTags.addAll(tags);
    }

    public boolean hasCompletedChallenge(String templateId) {
        return completedChallengeTemplates != null &&
                completedChallengeTemplates.contains(templateId);
    }

    public void incrementChallengeCount(String templateId) {
        if (templateCompletionCount == null) {
            templateCompletionCount = new HashMap<>();
        }
        templateCompletionCount.merge(templateId, 1, Integer::sum);
    }
}
