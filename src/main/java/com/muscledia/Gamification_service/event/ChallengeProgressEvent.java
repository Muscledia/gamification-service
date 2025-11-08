package com.muscledia.Gamification_service.event;


import com.muscledia.Gamification_service.model.UserChallenge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * PURPOSE: Event data for challenge progress update
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeProgressEvent extends BaseEvent {
    private Long userId;
    private String challengeId;
    private Integer currentProgress;
    private Integer targetValue;
    private Integer progressIncrement;
    private Double progressPercentage;
    private boolean justCompleted;
    private Instant updatedAt;


    @Override
    public String getEventType() {
        return "CHALLENGE_PROGRESS";
    }

    @Override
    public boolean isValid() {
        return isBaseValid() && challengeId != null;
    }

    @Override
    public BaseEvent withNewTimestamp() {
        return this.toBuilder()
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public double getIntensityScore() {
        return 0;
    }

    @Override
    public boolean isStreakEligible() {
        return false;
    }
}
