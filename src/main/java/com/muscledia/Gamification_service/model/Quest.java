package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.model.enums.QuestType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "quests")
public class Quest {

    @Id
    private String id;

    private String name;

    private String description;

    private QuestType questType;

    private ObjectiveType objectiveType;

    private int objectiveTarget;

    private int expReward;

    private int pointsReward;

    private Instant startDate;

    private Instant endDate;

    private boolean repeatable; // True if the quest can be done multiple times

    // Reference to exercise_id from workout-service's DB (if quest is
    // exercise-specific)
    private String exerciseId;

    // Reference to muscle_group_id from workout-service's DB (if quest is muscle
    // group-specific)
    private String muscleGroupId;

    private int requiredLevel; // Minimum user level to be eligible for this quest

    private Instant createdAt;
}
