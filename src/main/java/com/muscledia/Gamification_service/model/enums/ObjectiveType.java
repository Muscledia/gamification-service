package com.muscledia.Gamification_service.model.enums;

/**
 * PURPOSE: Define challenge objective types (aligned with existing system)
 * RESPONSIBILITY: Type safety for challenge objectives
 * COUPLING: None - pure enumeration
 */
public enum ObjectiveType {
    REPS,                    // Total reps completed
    DURATION,                // Minutes of activity
    EXERCISES,               // Number of unique exercises completed
    WEIGHT_LIFTED,           // Total weight lifted in kg/lbs
    TIME_BASED,              // Time-based challenges
    ACHIEVEMENT_BASED        // Streak maintenance, milestone achievements
}
