package com.muscledia.Gamification_service.model;

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

    // Flexible criteria for earning the champion status (e.g., {"type":
    // "PERSONAL_RECORD_WEIGHT", "targetWeightKg": 100})
    private Map<String, Object> criteria;

    private Instant createdAt;

    private Instant updatedAt;

}
