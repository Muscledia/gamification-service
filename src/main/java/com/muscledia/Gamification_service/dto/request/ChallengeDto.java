package com.muscledia.Gamification_service.dto.request;

import com.muscledia.Gamification_service.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Optimized Challenge DTO
 *
 * Includes ONLY what users need to:
 * 1. Understand the challenge
 * 2. Decide if they can do it
 * 3. Know what they'll get
 * 4. Track their progress
 * 5. Stay safe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {

    // ========== IDENTITY (What is this?) ==========
    private String id;
    private String name;
    private String description;

    // ========== CLASSIFICATION (What type of challenge?) ==========
    private ChallengeType type;              // DAILY/WEEKLY/MONTHLY - affects urgency
    private ChallengeCategory category;      // STRENGTH/CARDIO/CONSISTENCY - visual grouping
    private DifficultyLevel difficultyLevel; // Can I do this?
    private List<String> journeyTags;
    private String journeyPhase;

    // ========== GOAL (What do I need to do?) ==========
    private Integer targetValue;             // The goal number
    private String progressUnit;             // "reps", "minutes", "workouts", "kg"

    // ========== PROGRESS (How am I doing?) ==========
    private Integer currentProgress;         // Where user is now
    private Double completionPercentage;     // Visual progress (0-100)

    // ========== URGENCY (When do I need to finish?) ==========
    private String timeRemaining;            // "6 hours left", "2 days left" - human readable

    // ========== REWARDS (What's in it for me?) ==========
    private Integer rewardPoints;
    private Integer rewardCoins;
    private Integer experiencePoints;

    // ========== MOTIVATION (Why should I care?) ==========
    private Boolean isMilestone;             // Special achievement flag
    private Boolean isLegendary;             // Elite status flag
    private String completionMessage;        // Celebration message

    // ========== GUIDANCE (What should I know?) ==========
    private List<String> exerciseFocus;      // Which exercises to do
    private String safetyNote;               // Safety first!
    private List<String> tips;               // Helpful hints

    // ========== CONTEXT (Additional useful info) ==========
    private List<String> prerequisites;      // What needs to be done first (challenge names)
    private List<String> unlocks;            // What this unlocks (challenge names)
}