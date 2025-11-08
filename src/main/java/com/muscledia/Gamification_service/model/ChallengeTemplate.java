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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
}
