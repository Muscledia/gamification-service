package com.muscledia.Gamification_service.dto.response;

import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
public class ChallengeProgressResponse {
    private String id;
    private String challengeId;
    private String challengeName;
    private ChallengeType challengeType;
    private ChallengeStatus status;

    // Progress
    private Integer currentProgress;
    private Integer targetValue;
    private String progressUnit;
    private Double progressPercentage;
    private String progressDisplay;  // "45/50 reps"

    // Status indicators
    private boolean isNearCompletion;  // >= 75%
    private boolean isCompleted;
    private boolean isExpired;

    // Time tracking
    private Instant startedAt;
    private Instant expiresAt;
    private String timeRemaining;  // "2 hours left"
    private Integer daysActive;

    // Rewards
    private Integer pointsEarned;
    private boolean rewardClaimed;

    // UI helpers
    private String statusDisplay;
    private String statusColor;  // "green", "yellow", "red"

    public static ChallengeProgressResponse fromUserChallenge(UserChallenge userChallenge) {
        return ChallengeProgressResponse.builder()
                .id(userChallenge.getId())
                .challengeId(userChallenge.getChallengeId())
                .challengeName(userChallenge.getChallengeName())
                .challengeType(userChallenge.getChallengeType())
                .status(userChallenge.getStatus())
                .currentProgress(userChallenge.getCurrentProgress())
                .targetValue(userChallenge.getTargetValue())
                .progressUnit(userChallenge.getProgressUnit())
                .progressPercentage(userChallenge.getProgressPercentage())
                .progressDisplay(userChallenge.getProgressDisplay())
                .isNearCompletion(userChallenge.isNearCompletion())
                .isCompleted(userChallenge.getStatus() == ChallengeStatus.COMPLETED)
                .isExpired(userChallenge.isExpired())
                .startedAt(userChallenge.getStartedAt())
                .expiresAt(userChallenge.getExpiresAt())
                .timeRemaining(calculateTimeRemaining(userChallenge.getExpiresAt()))
                .daysActive(calculateDaysActive(userChallenge.getStartedAt()))
                .pointsEarned(userChallenge.getPointsEarned())
                .rewardClaimed(userChallenge.isRewardClaimed())
                .statusDisplay(userChallenge.getStatusDisplay())
                .statusColor(getStatusColor(userChallenge))
                .build();
    }

    private static String calculateTimeRemaining(Instant expiresAt) {
        if (expiresAt == null) return "No expiry";

        Duration duration = Duration.between(Instant.now(), expiresAt);
        if (duration.isNegative()) return "Expired";

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " hours left";
        }

        long days = duration.toDays();
        return days + " days left";
    }

    private static Integer calculateDaysActive(Instant startedAt) {
        if (startedAt == null) return 0;
        return (int) Duration.between(startedAt, Instant.now()).toDays();
    }

    private static String getStatusColor(UserChallenge challenge) {
        return switch (challenge.getStatus()) {
            case ACTIVE -> {
                if (challenge.isNearCompletion()) yield "green";
                if (challenge.getProgressPercentage() >= 50) yield "yellow";
                yield "blue";
            }
            case COMPLETED -> "green";
            case EXPIRED, FAILED -> "red";
        };
    }
}