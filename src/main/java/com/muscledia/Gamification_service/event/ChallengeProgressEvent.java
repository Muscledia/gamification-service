package com.muscledia.Gamification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeProgressEvent extends BaseEvent {

    // Core identifiers
    private String challengeId;
    private String challengeName;           // ⬅️ ADD THIS
    private String challengeType;           // ⬅️ ADD THIS

    // Progress tracking
    private Integer currentProgress;
    private Integer previousProgress;       // ⬅️ ADD THIS
    private Integer targetValue;
    private Integer progressIncrement;
    private Double progressPercentage;

    // Status
    private boolean justCompleted;

    @Override
    public String getEventType() {
        return "CHALLENGE_PROGRESS";
    }

    @Override
    public boolean isValid() {
        return isBaseValid() &&
                challengeId != null &&
                currentProgress != null &&
                targetValue != null;
    }

    @Override
    public BaseEvent withNewTimestamp() {
        return this.toBuilder()
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public double getIntensityScore() {
        return progressPercentage != null ? progressPercentage : 0.0;
    }

    @Override
    public boolean isStreakEligible() {
        return false;
    }

    // UI/UX Helper Methods
    public String getProgressMessage() {
        return String.format("Challenge '%s': %d/%d (%d%% complete)",
                challengeName,
                currentProgress,
                targetValue,
                progressPercentage.intValue());
    }

    public boolean isSignificantProgress() {
        return progressIncrement != null && progressIncrement >= (targetValue * 0.25);
    }
}