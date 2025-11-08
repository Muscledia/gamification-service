package com.muscledia.Gamification_service.dto.request;


import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * PURPOSE: Data transfer object for UserChallenge entity
 * RESPONSIBILITY: Expose user challenge progress to API clients
 * COUPLING: None - pure data transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChallengeDto {

    private String id;
    private String challengeId;
    private String challengeName;
    private String challengeType;
    private ChallengeStatus status;
    private Integer currentProgress;
    private Integer targetValue;
    private double progressPercentage;
    private String progressUnit;
    private Instant startedAt;
    private Instant completedAt;
    private Instant expiresAt;
    private Integer pointsEarned;

    // UI-friendly fields
    private String statusDisplayName;
    private String formattedProgress;
    private String timeRemaining;
    private boolean canComplete;
    private String completionMessage;
}
