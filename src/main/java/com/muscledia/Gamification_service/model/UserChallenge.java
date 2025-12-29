package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    private ChallengeStatus status = ChallengeStatus.ACTIVE;

    private String challengeName;
    private ChallengeType challengeType;
    private String progressUnit;  // "reps", "minutes", "exercises", "workouts"

    // Progress tracking
    private Integer currentProgress = 0;
    private Integer targetValue;

    // Timestamps
    private Instant startedAt;
    private Instant completedAt;
    private Instant expiresAt;
    private Instant lastUpdatedAt;  // ⬅️ ADD THIS FIELD
    private Instant createdAt;

    // Reward tracking
    private boolean rewardClaimed = false;
    private Integer pointsEarned = 0;
    private List<String> unlockedContent = new ArrayList<>();

    // Helper methods
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
        this.lastUpdatedAt = Instant.now();  // ⬅️ UPDATE TIMESTAMP

        // Auto-complete if target reached
        if (isTargetReached() && status == ChallengeStatus.ACTIVE) {
            this.status = ChallengeStatus.COMPLETED;
            this.completedAt = Instant.now();
        }
    }

    // UI/UX Helper Methods
    public String getStatusDisplay() {
        return switch (status) {
            case ACTIVE -> "In Progress";
            case COMPLETED -> "Completed";
            case FAILED -> "Failed";
            case EXPIRED -> "Expired";
        };
    }

    public String getProgressDisplay() {
        return String.format("%d/%d %s", currentProgress, targetValue, progressUnit);
    }

    public boolean isNearCompletion() {
        return getProgressPercentage() >= 75.0;
    }
}