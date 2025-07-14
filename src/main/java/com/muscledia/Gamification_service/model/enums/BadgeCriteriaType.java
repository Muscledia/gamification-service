package com.muscledia.Gamification_service.model.enums;

/**
 * Defines the types of criteria that can be used for badge requirements.
 * Each type corresponds to different achievement patterns and measurement
 * methods.
 */
public enum BadgeCriteriaType {

    // Workout-based criteria
    WORKOUT_COUNT, // Total number of workouts (e.g., "Complete 10 workouts")
    WORKOUT_STREAK, // Consecutive workout days (e.g., "7-day workout streak")
    WEEKLY_WORKOUTS, // Workouts per week (e.g., "5 workouts this week")
    MONTHLY_WORKOUTS, // Workouts per month (e.g., "20 workouts this month")

    // Exercise-specific criteria
    EXERCISE_COUNT, // Number of different exercises performed
    EXERCISE_MASTERY, // Mastery level of specific exercise
    EXERCISE_STREAK, // Consecutive days performing specific exercise

    // Performance-based criteria
    PERSONAL_RECORD, // New personal record achieved
    WEIGHT_LIFTED_TOTAL, // Total weight lifted over period
    WEIGHT_LIFTED_SESSION, // Weight lifted in single session
    REPS_COMPLETED, // Total reps completed

    // Time-based criteria
    WORKOUT_DURATION, // Total or average workout duration
    TRAINING_TIME_TOTAL, // Total training time over period

    // Progression criteria
    LEVEL_REACHED, // User level achievement
    POINTS_EARNED, // Total points earned
    STRENGTH_IMPROVEMENT, // Percentage strength improvement

    // Social/Community criteria
    CHALLENGES_COMPLETED, // Community challenges completed
    FRIENDS_MOTIVATED, // Number of friends motivated/helped

    // Seasonal/Special criteria
    SEASONAL_PARTICIPATION, // Participation in seasonal events
    MILESTONE_ACHIEVEMENT, // Special milestone reached

    // Consistency criteria
    LOGIN_STREAK, // Consecutive login days
    TRACKING_CONSISTENCY, // Consistent workout logging

    // Nutrition-related (if applicable)
    NUTRITION_TRACKING, // Consistent nutrition logging
    CALORIE_GOALS_MET // Meeting calorie/macro goals
}