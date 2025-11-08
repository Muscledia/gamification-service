package com.muscledia.Gamification_service.dto.request;

import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * PURPOSE: Data transfer object for Challenge entity
 * RESPONSIBILITY: Expose challenge data to API clients
 * COUPLING: None - pure data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {
    private String id;
    private String name;
    private String description;
    private ChallengeType type;
    private ObjectiveType objective;
    private Integer targetValue;
    private Integer rewardPoints;
    private String unlockedQuestId;
    private DifficultyLevel difficulty;
    private boolean autoEnroll;
    private Instant startDate;
    private Instant endDate;
    private boolean isActive;
    private String progressUnit;

    // UI-friendly fields
    private String formattedTarget;
    private String estimatedDuration;
    private boolean alreadyStarted;
}
