package com.muscledia.Gamification_service.event;

import com.muscledia.Gamification_service.mapper.ChallengeMapper;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * PURPOSE: Event data for challenge started
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeStartedEvent extends BaseEvent {

    private Long userId;
    private String challengeId;
    private String challengeName;
    private String challengeType;
    private Integer targetValue;
    private String progressUnit;
    private Instant startedAt;
    private Instant expiresAt;

    public static ChallengeStartedEvent of(UserChallenge userChallenge, Challenge challenge) {
        return ChallengeStartedEvent.builder()
                .userId(userChallenge.getUserId())
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .challengeType(challenge.getType().name())
                .targetValue(challenge.getTargetValue())

                .progressUnit(ChallengeMapper.getProgressUnit(challenge.getObjectiveType()))
                .startedAt(userChallenge.getStartedAt())
                .expiresAt(userChallenge.getExpiresAt())
                .timestamp(Instant.now())
                .build();
    }



    @Override
    public String getEventType() {
        return "CHALLENGE_STARTED";
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
