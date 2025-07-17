package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ChampionCriteriaType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "champions")
public class Champion {
    @Id
    private String id;

    @NotBlank(message = "Champion name is required")
    @Size(min = 3, max = 100, message = "Champion name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Champion description is required")
    @Size(max = 1000, message = "Champion description must not exceed 1000 characters")
    private String description;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String imageUrl;

    // Reference to exercise_id from workout-service's DB (if champion is
    // exercise-specific)
    @Size(max = 50, message = "Required exercise ID must not exceed 50 characters")
    private String requiredExerciseId;

    @Min(value = 1, message = "Base difficulty must be at least 1")
    @Max(value = 10, message = "Base difficulty must not exceed 10")
    private int baseDifficulty; // E.g., 1-5, for categorization

    // Reference to muscle_group_id from workout-service's DB (if champion is muscle
    // group-specific)
    @Size(max = 50, message = "Muscle group ID must not exceed 50 characters")
    private String muscleGroupId;

    // Type of criteria for earning the champion status (provides type safety and
    // validation)
    @NotNull(message = "Champion criteria type is required")
    private ChampionCriteriaType criteriaType;

    // Flexible criteria parameters for earning the champion status
    // (e.g., {"targetWeightKg": 100, "timeframeDays": 90, "bodyWeightMultiplier":
    // 2.0})
    @NotNull(message = "Criteria parameters are required")
    @Size(min = 1, message = "At least one criteria parameter is required")
    private Map<String, Object> criteriaParams;

    private Instant createdAt;

    private Instant updatedAt;

}
