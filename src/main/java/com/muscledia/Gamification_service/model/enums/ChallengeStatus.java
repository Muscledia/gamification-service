package com.muscledia.Gamification_service.model.enums;

import lombok.Getter;

@Getter
public enum ChallengeStatus {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    EXPIRED("Expired"),
    FAILED("Failed");

    private final String displayName;

    ChallengeStatus(String displayName) {
        this.displayName = displayName;
    }

}
