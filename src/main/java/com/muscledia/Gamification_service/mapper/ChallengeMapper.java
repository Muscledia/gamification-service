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
    public static ChallengeDto toDto(Challenge challenge) {
        if (challenge == null) {
            return null;
        }

        return ChallengeDto.builder()
                .id(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .type(challenge.getType())
                .objective(challenge.getObjectiveType())
                .targetValue(challenge.getTargetValue())
                .rewardPoints(challenge.getRewardPoints())
                .unlockedQuestId(challenge.getUnlockedQuestId())
                .difficulty(challenge.getDifficultyLevel())
                .autoEnroll(challenge.isAutoEnroll())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .isActive(challenge.isActive())
                .progressUnit(getProgressUnit(challenge.getObjectiveType()))
                .formattedTarget(formatTarget(challenge.getTargetValue(), challenge.getObjectiveType()))
                .estimatedDuration(challenge.getType().getDisplayName())
                .build();
    }

    public static Challenge toEntity(ChallengeDto dto) {
        if (dto == null) {
            return null;
        }

        return Challenge.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .objectiveType(dto.getObjective())
                .targetValue(dto.getTargetValue())
                .rewardPoints(dto.getRewardPoints())
                .unlockedQuestId(dto.getUnlockedQuestId())
                .difficultyLevel(dto.getDifficulty())
                .autoEnroll(dto.isAutoEnroll())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
    }

    public static String getProgressUnit(ObjectiveType objective) {
        return switch (objective) {
            case REPS -> "reps";
            case DURATION -> "minutes";
            case EXERCISES -> "exercises";
            case WEIGHT_LIFTED -> "kg";
            case TIME_BASED -> "workouts";
            case ACHIEVEMENT_BASED -> "achievements";
            default -> "points";
        };
    }

    private static String formatTarget(Integer targetValue, ObjectiveType objective) {
        if (targetValue == null) return "0";

        return switch (objective) {
            case REPS -> targetValue + " reps";
            case DURATION -> targetValue + " minutes";
            case EXERCISES -> targetValue + " exercises";
            case WEIGHT_LIFTED -> targetValue + " kg";
            case TIME_BASED -> targetValue + " workouts";
            case ACHIEVEMENT_BASED -> targetValue + " achievements";
            default -> targetValue + " points";
        };
    }
}
