package com.muscledia.Gamification_service.model.enums;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
public enum ChallengeType {

    DAILY(Duration.ofDays(1), "Daily Challenge"),
    WEEKLY(Duration.ofDays(7), "Weekly Challenge"),
    MONTHLY(Duration.ofDays(30), "Monthly Challenge"),
    YEARLY(Duration.ofDays(365), "Yearly Challenge");

    private final Duration duration;
    private final String displayName;

    ChallengeType(Duration duration, String displayName) {
        this.duration = duration;
        this.displayName = displayName;
    }

    public Instant calculateExpiryTime(Instant startTime) {
        return startTime.plus(duration);
    }
}
