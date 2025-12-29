package com.muscledia.Gamification_service.model.enums;

/**
 * Challenge objective types
 * Defines what metric a challenge tracks
 */
public enum ObjectiveType {
    EXERCISES,           // Complete X unique exercises
    REPS,                // Complete X total reps
    DURATION,            // Exercise for X minutes
    TIME_BASED,          // Complete X workouts
    ACHIEVEMENT_BASED,   // Maintain streaks, consistency
    VOLUME_BASED,        // Lift X kg total volume (also covers WEIGHT_LIFTED)
    CALORIES,            // Burn X calories
    PERSONAL_RECORDS     // Achieve X personal records
}