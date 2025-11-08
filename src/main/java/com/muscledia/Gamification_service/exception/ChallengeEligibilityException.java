package com.muscledia.Gamification_service.exception;

/**
 * PURPOSE: Exception for when user cannot participate in a challenge
 */
public class ChallengeEligibilityException extends BusinessException {

    public ChallengeEligibilityException(String message) {
        super(message);
    }

    public ChallengeEligibilityException(Long userId, String challengeId, String reason) {
        super(String.format("User %d cannot participate in challenge %s: %s",
                userId, challengeId, reason));
    }
}