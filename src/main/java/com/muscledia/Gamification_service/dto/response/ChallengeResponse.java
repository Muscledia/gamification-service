package com.muscledia.Gamification_service.dto.response;

import com.muscledia.Gamification_service.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive challenge data for frontend
 * Contains all information needed for UI rendering and progress tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {

    // ========== IDENTITY ==========
    private String id;
    private String templateId;
    private String name;
    private String description;

    // ========== CLASSIFICATION ==========
    private ChallengeType type;              // DAILY, WEEKLY, MONTHLY
    private ChallengeCategory category;      // STRENGTH, CARDIO, CONSISTENCY
    private ObjectiveType objectiveType;     // REPS, DURATION, EXERCISES
    private DifficultyLevel difficultyLevel; // BEGINNER, INTERMEDIATE, ADVANCED, ELITE

    // ========== TARGETS & REWARDS ==========
    private Integer targetValue;
    private Integer rewardPoints;
    private Integer rewardCoins;
    private Integer experiencePoints;
    private String progressUnit;             // "reps", "minutes", "kg"

    // ========== PROGRESS TRACKING ==========
    private Integer currentProgress;
    private Double completionPercentage;
    private String status;                   // "NOT_STARTED", "IN_PROGRESS", "COMPLETED", "FAILED"
    private Instant startedAt;
    private Instant expiresAt;
    private Integer daysRemaining;

    // ========== JOURNEY & PREREQUISITES ==========
    private String journeyPhase;             // "foundation", "building", "mastery"
    private Set<String> journeyTags;
    private List<PrerequisiteInfo> prerequisites;
    private List<UnlockInfo> unlocks;
    private Boolean isEligible;
    private String ineligibilityReason;

    // ========== UI FLAGS ==========
    private Boolean isMilestone;
    private Boolean isLegendary;
    private Boolean autoEnroll;

    // ========== METADATA ==========
    private String completionMessage;
    private List<String> tips;
    private Map<String, Object> metadata;

    // ========== EXERCISE-SPECIFIC ==========
    private List<String> exerciseFocus;      // Target exercises for this challenge
    private String safetyNote;               // Safety instructions if any
    private List<String> movementPatterns;   // Movement patterns (squat, hinge, push, pull, etc.)

    // ========== ANALYTICS ==========
    private Integer attemptCount;
    private Integer globalCompletionRate;    // Percentage of users who completed
    private String popularityRank;           // "Most Popular", "Trending"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrerequisiteInfo {
        private String challengeId;
        private String challengeName;
        private Boolean isCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnlockInfo {
        private String type;                 // "CHALLENGE", "BADGE", "QUEST"
        private String id;
        private String name;
        private String description;
    }
}