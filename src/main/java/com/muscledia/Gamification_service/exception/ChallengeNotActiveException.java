package com.muscledia.Gamification_service.exception;

/**
 * PURPOSE: Exception for when challenge is not in a valid state for the operation
 */
public class ChallengeNotActiveException extends BusinessException {

    public ChallengeNotActiveException(String challengeId) {
        super("Challenge is not active: " + challengeId);
    }
}
