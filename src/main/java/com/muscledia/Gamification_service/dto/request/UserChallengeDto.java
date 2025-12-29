package com.muscledia.Gamification_service.dto.request;

import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserChallengeDto {
    private String id;
    private Long userId;
    private String challengeId;
    private String challengeName;
    private ChallengeType challengeType;
    private ChallengeStatus status;
    private Integer currentProgress;
    private Integer targetValue;
    private String progressUnit;
    private Double progressPercentage;
    private String progressDisplay;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant completedAt;
    private String timeRemaining;
    private boolean isNearCompletion;
    private Integer pointsEarned;
    private String statusDisplay;
    private String statusColor;
}