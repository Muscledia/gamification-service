package com.muscledia.Gamification_service.mapper;


import com.muscledia.Gamification_service.dto.request.ChallengeDto;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;

/**
 * PURPOSE: Convert between Challenge entity and DTO
 * RESPONSIBILITY: Data transformation for API layer
 * COUPLING: Low - only knows about entity and DTO
 */
public class ChallengeMapper {

    /**
     * Get user-friendly progress unit based on objective type
     */
    public static String getProgressUnit(ObjectiveType objectiveType) {
        if (objectiveType == null) {
            return "points";
        }

        return switch (objectiveType) {
            case EXERCISES -> "exercises";
            case REPS -> "reps";
            case DURATION -> "minutes";
            case TIME_BASED -> "workouts";
            case ACHIEVEMENT_BASED -> "achievements";
            case VOLUME_BASED -> "kg";
            case CALORIES -> "calories";
            case PERSONAL_RECORDS -> "PRs";
        };
    }

    /**
     * Get display name for objective type
     */
    public static String getObjectiveDisplayName(ObjectiveType objectiveType) {
        if (objectiveType == null) {
            return "Unknown";
        }

        return switch (objectiveType) {
            case EXERCISES -> "Complete Exercises";
            case REPS -> "Complete Reps";
            case DURATION -> "Exercise Duration";
            case TIME_BASED -> "Workout Count";
            case ACHIEVEMENT_BASED -> "Maintain Streak";
            case VOLUME_BASED -> "Lift Volume";
            case CALORIES -> "Burn Calories";
            case PERSONAL_RECORDS -> "Break Personal Records";
        };
    }

    /**
     * Convert Challenge entity to DTO
     */
    public static ChallengeDto toDto(Challenge challenge) {
        if (challenge == null) {
            return null;
        }

        return ChallengeDto.builder()
                .id(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .type(challenge.getType())
                .category(challenge.getCategory())
                .objectiveType(challenge.getObjectiveType())
                .targetValue(challenge.getTargetValue())
                .rewardPoints(challenge.getRewardPoints())
                .difficultyLevel(challenge.getDifficultyLevel())
                .progressUnit(getProgressUnit(challenge.getObjectiveType()))
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .active(challenge.isActive())
                .build();
    }
}
