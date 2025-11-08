package com.muscledia.Gamification_service.exception;

/**
 * PURPOSE: Exception for when user tries to start a challenge they already started
 */
public class ChallengeAlreadyStartedException extends BusinessException {

    public ChallengeAlreadyStartedException(String challengeId) {
        super("Challenge already started: " + challengeId);
    }
}
