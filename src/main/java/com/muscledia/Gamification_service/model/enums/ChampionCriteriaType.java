package com.muscledia.Gamification_service.model.enums;

/**
 * Defines the types of criteria that can be used for champion-level
 * achievements.
 * Champions represent elite accomplishments requiring significant dedication
 * and skill.
 */
public enum ChampionCriteriaType {

    // Elite Performance criteria
    PERSONAL_RECORD_WEIGHT, // Achieve specific weight in exercise (e.g., "Bench 100kg")
    PERSONAL_RECORD_REPS, // Achieve specific rep count (e.g., "100 push-ups")
    STRENGTH_MILESTONE, // Major strength achievement (e.g., "2x bodyweight squat")
    ENDURANCE_MILESTONE, // Endurance achievement (e.g., "Run 10km under 45min")

    // Exercise Mastery criteria
    EXERCISE_PERFECTIONIST, // Perfect form over extended period
    EXERCISE_DEDICATION, // Consistent performance of specific exercise
    TECHNICAL_MASTERY, // Advanced technique demonstration
    EXERCISE_INNOVATION, // Creative exercise variations

    // Long-term Achievement criteria
    CONSISTENCY_CHAMPION, // Long-term consistency (e.g., "365-day workout streak")
    TRANSFORMATION_MASTER, // Dramatic physical transformation
    STRENGTH_PROGRESSION, // Percentage improvement over time
    ENDURANCE_PROGRESSION, // Cardiovascular improvement

    // Competitive criteria
    COMPETITION_WINNER, // Win in platform competitions
    LEADERBOARD_DOMINATION, // Top position for extended period
    RECORD_BREAKER, // Break platform records

    // Mentorship/Community criteria
    COMMUNITY_LEADER, // Help and mentor other users
    MOTIVATION_MASTER, // Inspire high engagement in others
    KNOWLEDGE_SHARER, // Contribute valuable content/advice

    // Specialized Achievement criteria
    MUSCLE_GROUP_SPECIALIST, // Master all exercises for muscle group
    WORKOUT_TYPE_MASTER, // Excel in specific workout category
    RECOVERY_EXPERT, // Optimal recovery and rest patterns

    // Elite Dedication criteria
    TRAINING_VOLUME_ELITE, // Exceptional training volume
    INTENSITY_MASTER, // Consistently high training intensity
    DISCIPLINE_EXEMPLAR, // Unwavering commitment to routine

    // Advanced Performance criteria
    POWER_TO_WEIGHT_RATIO, // Exceptional strength-to-weight ratio
    BALANCED_DEVELOPMENT, // Even development across all areas
    FUNCTIONAL_STRENGTH, // Real-world strength application

    // Longevity criteria
    VETERAN_ACHIEVER, // Long-term platform engagement
    SUSTAINED_EXCELLENCE, // Maintaining high performance over time
    COMEBACK_CHAMPION // Outstanding recovery from setback
}