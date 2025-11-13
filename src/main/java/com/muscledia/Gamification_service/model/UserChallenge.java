package com.muscledia.Gamification_service.model;


import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


/**
 * PURPOSE: Track individual user progress on challenges
 * RESPONSIBILITY: Manage progress state and completion logic
 * COUPLING: None - pure domain object
 */
@Document(collection = "user_challenges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChallenge {

    @Id
    private String id;

    @NotNull
    private Long userId;

    @NotNull
    private String challengeId;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status = ChallengeStatus.ACTIVE; // ACTIVE, COMPLETED, FAILED, EXPIRED

    private String challengeName;
    private ChallengeType challengeType;
    private String progressUnit;

    // Progress tracking
    private Integer currentProgress = 0;
    private Integer targetValue;

    // Timestamps
    private Instant startedAt;
    private Instant completedAt;
    private Instant expiresAt;

    // Reward tracking
    private boolean rewardClaimed = false;
    private Integer pointsEarned = 0;
    private List<String> unlockedContent = new ArrayList<>();

    // Metadata
    private Instant createdAt;


    public double getProgressPercentage() {
        if (targetValue == null || targetValue == 0) return 0.0;
        return Math.min(100.0, (double) currentProgress / targetValue * 100.0);
    }

    public boolean isTargetReached() {
        return currentProgress != null && targetValue != null &&
                currentProgress >= targetValue;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public void addProgress(int increment) {
        this.currentProgress = (currentProgress != null ? currentProgress : 0) + increment;

        // Auto-complete if target reached
        if (isTargetReached() && status == ChallengeStatus.ACTIVE) {
            this.status = ChallengeStatus.COMPLETED;
            this.completedAt = Instant.now();
        }
    }
}
