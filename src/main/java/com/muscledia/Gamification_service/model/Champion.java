package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ChampionCriteriaType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "champions")
public class Champion {
    @Id
    private String id;

    private String name;

    private String description;

    private String imageUrl;

    // Reference to exercise_id from workout-service's DB (if champion is
    // exercise-specific)
    private String requiredExerciseId;

    private int baseDifficulty; // E.g., 1-5, for categorization

    // Reference to muscle_group_id from workout-service's DB (if champion is muscle
    // group-specific)
    private String muscleGroupId;

    // Type of criteria for earning the champion status (provides type safety and
    // validation)
    private ChampionCriteriaType criteriaType;

    // Flexible criteria parameters for earning the champion status
    // (e.g., {"targetWeightKg": 100, "timeframeDays": 90, "bodyWeightMultiplier":
    // 2.0})
    private Map<String, Object> criteriaParams;

    private Instant createdAt;

    private Instant updatedAt;

}
