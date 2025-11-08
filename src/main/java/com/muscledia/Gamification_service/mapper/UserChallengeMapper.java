package com.muscledia.Gamification_service.mapper;


import com.muscledia.Gamification_service.dto.request.UserChallengeDto;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;

import java.time.Duration;
import java.time.Instant;

/**
 * PURPOSE: Convert between UserChallenge entity and DTO
 * RESPONSIBILITY: Data transformation for API layer
 * COUPLING: Low - only knows about entity and DTO
 */
public class UserChallengeMapper {

    public static UserChallengeDto toDto(UserChallenge userChallenge) {
        return toDto(userChallenge, null);
    }

    public static UserChallengeDto toDto(UserChallenge userChallenge, Challenge challenge) {
        if (userChallenge == null) {
            return null;
        }

        return UserChallengeDto.builder()
                .id(userChallenge.getId())
                .challengeId(userChallenge.getChallengeId())
                .challengeName(challenge != null ? challenge.getName() : "Unknown Challenge")
                .challengeType(challenge != null ? challenge.getType().name() : "UNKNOWN")
                .status(userChallenge.getStatus())
                .currentProgress(userChallenge.getCurrentProgress())
                .targetValue(userChallenge.getTargetValue())
                .progressPercentage(userChallenge.getProgressPercentage())
                // FIX: Use the public method from ChallengeMapper
                .progressUnit(challenge != null ? ChallengeMapper.getProgressUnit(challenge.getObjectiveType()) : "points")
                .startedAt(userChallenge.getStartedAt())
                .completedAt(userChallenge.getCompletedAt())
                .expiresAt(userChallenge.getExpiresAt())
                .pointsEarned(challenge != null ? challenge.getRewardPoints() : 0)
                .statusDisplayName(userChallenge.getStatus().getDisplayName())
                .formattedProgress(formatProgress(userChallenge, challenge))
                .timeRemaining(calculateTimeRemaining(userChallenge.getExpiresAt()))
                .canComplete(userChallenge.isTargetReached())
                .completionMessage(challenge != null ? generateCompletionMessage(challenge) : "Challenge completed!")
                .build();
    }

    private static String formatProgress(UserChallenge userChallenge, Challenge challenge) {
        if (challenge == null) {
            return userChallenge.getCurrentProgress() + "/" + userChallenge.getTargetValue();
        }

        String unit = ChallengeMapper.getProgressUnit(challenge.getObjectiveType());
        return userChallenge.getCurrentProgress() + "/" + userChallenge.getTargetValue() + " " + unit;
    }

    private static String calculateTimeRemaining(Instant expiresAt) {
        if (expiresAt == null) {
            return "No deadline";
        }

        Instant now = Instant.now();
        if (now.isAfter(expiresAt)) {
            return "Expired";
        }

        Duration remaining = Duration.between(now, expiresAt);
        long hours = remaining.toHours();
        long minutes = remaining.toMinutes() % 60;

        if (hours > 24) {
            return (hours / 24) + " days remaining";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m remaining";
        } else {
            return minutes + "m remaining";
        }
    }

    private static String generateCompletionMessage(Challenge challenge) {
        return "Congratulations! You completed the " + challenge.getName() + " challenge!";
    }
}
