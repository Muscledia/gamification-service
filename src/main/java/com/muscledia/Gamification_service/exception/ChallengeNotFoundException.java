package com.muscledia.Gamification_service.exception;

public class ChallengeNotFoundException extends ResourceNotFoundException {
  public ChallengeNotFoundException(String challengeId) {
    super("Challenge not found with ID: " + challengeId);
  }

  public ChallengeNotFoundException(String challengeId, Throwable cause) {
    super("Challenge not found with ID: " + challengeId, cause);
  }
}
