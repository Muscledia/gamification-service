package com.muscledia.Gamification_service.dto.request;

import com.muscledia.Gamification_service.model.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ChallengeDto {
    private String id;
    private String name;
    private String description;
    private ChallengeType type;
    private ChallengeCategory category;
    private ObjectiveType objectiveType;
    private Integer targetValue;
    private Integer rewardPoints;
    private DifficultyLevel difficultyLevel;
    private String progressUnit;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
}