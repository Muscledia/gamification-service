package com.muscledia.Gamification_service.model;


import com.muscledia.Gamification_service.model.enums.ChallengeCategory;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * PURPOSE: Core challenge entity representing a fitness objective
 * RESPONSIBILITY: Hold challenge data and basic business rules
 * COUPLING: None - pure domain object
 */
@Document(collection = "challenges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {

    @Id
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @Enumerated(EnumType.STRING)
    private ChallengeType type; // DAILY, WEEKLY

    @Enumerated(EnumType.STRING)
    private ChallengeCategory category; // STRENGTH, CARDIO, CONSISTENCY

    @Enumerated(EnumType.STRING)
    private ObjectiveType objectiveType; // CALORIES, WORKOUTS, REPS

    @Min(1)
    private Integer targetValue;

    @Min(0)
    private Integer rewardPoints;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    // Quest unlocking
    private String unlockedQuestId;
    private String unlockedBadgeId;

    private boolean autoEnroll = false; // Auto-enroll users in this challenge

    // Challenge availability
    private boolean active = true;
    private Instant startDate;
    private Instant endDate;

    // Progress tracking
    private String progressUnit; // "calories", "minutes", "reps"
    private String completionMessage;

    // Metadata
    private Instant createdAt;
    private Instant updatedAt;

    private List<String> prerequisiteChallengeIds;
    private List<String> unlocksChallengeIds;
    private Set<String> userJourneyTags; // "strength", "endurance", "beginner"
    private String journeyPhase; // "foundation", "building", "mastery"

    // ADD PERSONALIZATION FIELDS
    private String templateId; // Reference to template used
    private Double personalizedDifficultyMultiplier = 1.0;
    private Map<String, Object> personalizationData; // User-specific adjustments

    // ADD ANALYTICS FIELDS
    private int expectedCompletionRate = 70; // Target completion rate %
    private int actualCompletionCount = 0;
    private int attemptCount = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Business methods - no external dependencies
    public boolean isExpired() {
        return Instant.now().isAfter(endDate);
    }

    public boolean isActive() {
        Instant now = Instant.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    public Duration getDuration() {
        return Duration.between(startDate, endDate);
    }

    // ADD BUSINESS METHODS
    public boolean hasPrerequisites() {
        return prerequisiteChallengeIds != null && !prerequisiteChallengeIds.isEmpty();
    }

    public boolean isEligibleFor(UserJourneyProfile userJourney) {
        // Check level requirement
        if (getDifficultyLevel().ordinal() > userJourney.getCurrentLevel() / 3) {
            return false;
        }

        // Check prerequisites
        if (hasPrerequisites()) {
            return userJourney.getCompletedChallengeTemplates()
                    .containsAll(prerequisiteChallengeIds);
        }

        // Check journey alignment
        if (!getUserJourneyTags().isEmpty()) {
            return !Collections.disjoint(getUserJourneyTags(),
                    userJourney.getActiveJourneyTags());
        }

        return true;
    }

    public double getCurrentCompletionRate() {
        return attemptCount > 0 ? (double) actualCompletionCount / attemptCount * 100 : 0;
    }
}
