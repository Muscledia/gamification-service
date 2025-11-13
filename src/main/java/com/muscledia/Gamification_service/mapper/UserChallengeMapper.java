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

        UserChallengeDto.UserChallengeDtoBuilder builder = UserChallengeDto.builder()
                .id(userChallenge.getId())
                .challengeId(userChallenge.getChallengeId())
                .status(userChallenge.getStatus())
                .currentProgress(userChallenge.getCurrentProgress())
                .targetValue(userChallenge.getTargetValue())
                .progressPercentage(calculateProgressPercentage(userChallenge))
                .startedAt(userChallenge.getStartedAt())
                .completedAt(userChallenge.getCompletedAt())
                .expiresAt(userChallenge.getExpiresAt())
                .pointsEarned(userChallenge.getPointsEarned())
                .statusDisplayName(userChallenge.getStatus().getDisplayName())
                .formattedProgress(formatProgress(userChallenge))
                .timeRemaining(calculateTimeRemaining(userChallenge))
                .canComplete(userChallenge.isTargetReached())
                .completionMessage(userChallenge.isTargetReached() ? "Challenge completed!" : "Keep going!");

        // If challenge details are provided, use them
        if (challenge != null) {
            builder.challengeName(challenge.getName())
                    .challengeType(challenge.getType().name())
                    .progressUnit(ChallengeMapper.getProgressUnit(challenge.getObjectiveType()));
        } else {
            // Fallback to generic values
            builder.challengeName("Challenge")
                    .challengeType("UNKNOWN")
                    .progressUnit("points");
        }

        return builder.build();
    }

    private static double calculateProgressPercentage(UserChallenge userChallenge) {
        if (userChallenge.getTargetValue() == 0) return 0.0;
        return (double) userChallenge.getCurrentProgress() / userChallenge.getTargetValue() * 100;
    }

    private static String formatProgress(UserChallenge userChallenge) {
        return userChallenge.getCurrentProgress() + "/" + userChallenge.getTargetValue();
    }

    private static String calculateTimeRemaining(UserChallenge userChallenge) {
        if (userChallenge.getExpiresAt() == null) return "No deadline";

        long seconds = java.time.Duration.between(
                java.time.Instant.now(),
                userChallenge.getExpiresAt()
        ).getSeconds();

        if (seconds <= 0) return "Expired";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        return hours + "h " + minutes + "m remaining";
    }

    private static String generateCompletionMessage(Challenge challenge) {
        return "Congratulations! You completed the " + challenge.getName() + " challenge!";
    }
}
