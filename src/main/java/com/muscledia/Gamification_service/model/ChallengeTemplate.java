package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.model.enums.ChallengeCategory;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.*;


/**
 * PURPOSE: Template for generating challenges with different difficulty levels
 * RESPONSIBILITY: Define challenge structure with scaling values
 * COUPLING: None - pure domain object
 */
@Document(collection = "challenge_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeTemplate {

    @Id
    private String id;

    private String name;
    private String description;
    private ChallengeType type;
    private ObjectiveType objective;
    private String unlockedQuestId;

    private List<String> prerequisiteTemplates;
    private List<String> unlocksTemplates;
    private Set<String> userJourneyTags;
    private String journeyPhase;
    private Map<String, Object> metadata = new HashMap<>();

    // Target values for each difficulty level
    @Builder.Default
    private Map<DifficultyLevel, Integer> targetValues = new HashMap<>();

    // Reward points for each difficulty level
    @Builder.Default
    private Map<DifficultyLevel, Integer> rewardPoints = new HashMap<>();

    // Template metadata
    private boolean active = true;
    private double weight = 1.0; // For random selection
    private Instant createdAt;

    // Helper methods
    public Integer getTargetValue(DifficultyLevel difficulty) {
        return targetValues.getOrDefault(difficulty, 100); // Default fallback
    }

    public Integer getRewardPoints(DifficultyLevel difficulty) {
        return rewardPoints.getOrDefault(difficulty, 50); // Default fallback
    }

    public String getJourneyPhase() {
        return journeyPhase != null ? journeyPhase : "foundation";
    }

    public List<String> getPrerequisiteTemplates() {
        return prerequisiteTemplates != null ? prerequisiteTemplates : new ArrayList<>();
    }

    public List<String> getUnlocksTemplates() {
        return unlocksTemplates != null ? unlocksTemplates : new ArrayList<>();
    }

    public Set<String> getUserJourneyTags() {
        return userJourneyTags != null ? userJourneyTags : new HashSet<>();
    }

    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    public boolean isMilestone() {
        return metadata != null && Boolean.TRUE.equals(metadata.get("milestone"));
    }

    public boolean isLegendary() {
        return metadata != null && Boolean.TRUE.equals(metadata.get("legendary"));
    }
}
