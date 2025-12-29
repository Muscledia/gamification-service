package com.muscledia.Gamification_service.event;

import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * PURPOSE: Event data for challenge completion
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeCompletedEvent extends BaseEvent {
    private Long userId;
    private String challengeId;
    private String challengeName;
    private String challengeType;
    private Integer pointsAwarded;
    private String unlockedQuestId;
    private Instant completedAt;
    private Integer finalProgress;
    private Integer targetValue;
    private Integer timeTakenHours;


    @Override
    public String getEventType() {
        return "CHALLENGE_COMPLETED";
    }

    @Override
    public boolean isValid() {
        return isBaseValid() && challengeId != null && challengeName != null;
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
