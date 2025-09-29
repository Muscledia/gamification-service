package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.model.enums.QuestType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.time.Instant;

@Data
@Document(collection = "quests")
public class Quest {

    @Id
    private String id;

    @NotBlank(message = "Quest name is required")
    @Size(min = 3, max = 100, message = "Quest name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Quest description is required")
    @Size(max = 1000, message = "Quest description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Quest type is required")
    private QuestType questType;

    @NotNull(message = "Objective type is required")
    private ObjectiveType objectiveType;

    @Min(value = 1, message = "Objective target must be positive")
    @Max(value = 1000000, message = "Objective target must not exceed 1,000,000")
    private int objectiveTarget;

    @Min(value = 0, message = "Experience reward must be non-negative")
    @Max(value = 10000, message = "Experience reward must not exceed 10,000")
    private int expReward;

    @Min(value = 0, message = "Points reward must be non-negative")
    @Max(value = 10000, message = "Points reward must not exceed 10,000")
    private int pointsReward;

    @NotNull(message = "Quest start date is required")
    private Instant startDate;

    @NotNull(message = "Quest end date is required")
    private Instant endDate;

    private boolean repeatable; // True if the quest can be done multiple times

    // Reference to exercise_id from workout-service's DB (if quest is
    // exercise-specific)
    @Size(max = 50, message = "Exercise ID must not exceed 50 characters")
    private String exerciseId;

    // Reference to muscle_group_id from workout-service's DB (if quest is muscle
    // group-specific)
    @Size(max = 50, message = "Muscle group ID must not exceed 50 characters")
    private String muscleGroupId;

    @Min(value = 1, message = "Required level must be at least 1")
    @Max(value = 100, message = "Required level must not exceed 100")
    private int requiredLevel; // Minimum user level to be eligible for this quest

    private Instant createdAt;
}
