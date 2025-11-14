package com.muscledia.Gamification_service.event;

import com.muscledia.Gamification_service.mapper.ChallengeMapper;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Challenge ID cannot be blank")
    private String challengeId;

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotBlank(message = "Challenge name cannot be blank")
    private String challengeName;

    @NotNull(message = "Challenge type cannot be null")
    private ChallengeType challengeType;

    @NotNull(message = "Started at cannot be null")
    private Instant startedAt;

    public static ChallengeStartedEvent of(UserChallenge userChallenge, Challenge challenge) {
        if (userChallenge == null) {
            throw new IllegalArgumentException("UserChallenge cannot be null");
        }
        if (challenge == null) {
            throw new IllegalArgumentException("Challenge cannot be null");
        }

        // Add null safety checks
        String challengeName = challenge.getName() != null ? challenge.getName() : "Unknown Challenge";
        ChallengeType challengeType = challenge.getType() != null ? challenge.getType() : ChallengeType.DAILY;
        Instant startedAt = userChallenge.getStartedAt() != null ? userChallenge.getStartedAt() : Instant.now();
        Long userId = userChallenge.getUserId();
        String challengeId = userChallenge.getChallengeId();

        // Validate required fields before creating event
        if (userId == null) {
            throw new IllegalArgumentException("UserChallenge.userId cannot be null");
        }
        if (challengeId == null || challengeId.trim().isEmpty()) {
            throw new IllegalArgumentException("UserChallenge.challengeId cannot be null or empty");
        }

        return ChallengeStartedEvent.builder()
                .challengeId(challengeId)
                .userId(userId)
                .challengeName(challengeName)
                .challengeType(challengeType)
                .startedAt(startedAt)
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
