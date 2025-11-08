package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


// Example challenge templates data for initial setup
public class ChallengeTemplateData {
    public static List<ChallengeTemplate> getDefaultTemplates() {
        return Arrays.asList(

                // Daily Challenges
                ChallengeTemplate.builder()
                        .name("Rep Master")
                        .description("Complete the target number of reps across all exercises")
                        .type(ChallengeType.DAILY)
                        .objective(ObjectiveType.REPS)
                        .targetValues(Map.of(
                                DifficultyLevel.BEGINNER, 50,
                                DifficultyLevel.INTERMEDIATE, 100,
                                DifficultyLevel.ADVANCED, 150,
                                DifficultyLevel.ELITE, 200
                        ))
                        .rewardPoints(Map.of(
                                DifficultyLevel.BEGINNER, 50,
                                DifficultyLevel.INTERMEDIATE, 75,
                                DifficultyLevel.ADVANCED, 100,
                                DifficultyLevel.ELITE, 125
                        ))
                        .unlockedQuestId("strength-warrior-quest")
                        .weight(1.0)
                        .build(),

                ChallengeTemplate.builder()
                        .name("Time Champion")
                        .description("Exercise for the target duration")
                        .type(ChallengeType.DAILY)
                        .objective(ObjectiveType.DURATION)
                        .targetValues(Map.of(
                                DifficultyLevel.BEGINNER, 20,
                                DifficultyLevel.INTERMEDIATE, 30,
                                DifficultyLevel.ADVANCED, 45,
                                DifficultyLevel.ELITE, 60
                        ))
                        .rewardPoints(Map.of(
                                DifficultyLevel.BEGINNER, 60,
                                DifficultyLevel.INTERMEDIATE, 90,
                                DifficultyLevel.ADVANCED, 120,
                                DifficultyLevel.ELITE, 150
                        ))
                        .unlockedQuestId("endurance-hero-quest")
                        .weight(1.0)
                        .build(),

                ChallengeTemplate.builder()
                        .name("Exercise Explorer")
                        .description("Try different exercises in your workout")
                        .type(ChallengeType.DAILY)
                        .objective(ObjectiveType.EXERCISES)
                        .targetValues(Map.of(
                                DifficultyLevel.BEGINNER, 3,
                                DifficultyLevel.INTERMEDIATE, 5,
                                DifficultyLevel.ADVANCED, 7,
                                DifficultyLevel.ELITE, 10
                        ))
                        .rewardPoints(Map.of(
                                DifficultyLevel.BEGINNER, 40,
                                DifficultyLevel.INTERMEDIATE, 60,
                                DifficultyLevel.ADVANCED, 80,
                                DifficultyLevel.ELITE, 100
                        ))
                        .unlockedQuestId("variety-master-quest")
                        .weight(1.0)
                        .build(),

                ChallengeTemplate.builder()
                        .name("Consistency Champion")
                        .description("Maintain your workout streak")
                        .type(ChallengeType.DAILY)
                        .objective(ObjectiveType.ACHIEVEMENT_BASED)
                        .targetValues(Map.of(
                                DifficultyLevel.BEGINNER, 1,
                                DifficultyLevel.INTERMEDIATE, 1,
                                DifficultyLevel.ADVANCED, 1,
                                DifficultyLevel.ELITE, 1
                        ))
                        .rewardPoints(Map.of(
                                DifficultyLevel.BEGINNER, 30,
                                DifficultyLevel.INTERMEDIATE, 40,
                                DifficultyLevel.ADVANCED, 50,
                                DifficultyLevel.ELITE, 60
                        ))
                        .unlockedQuestId("discipline-master-quest")
                        .weight(1.0)
                        .build(),

                // Weekly Challenges
                ChallengeTemplate.builder()
                        .name("Weekly Warrior")
                        .description("Complete workouts consistently this week")
                        .type(ChallengeType.WEEKLY)
                        .objective(ObjectiveType.TIME_BASED)
                        .targetValues(Map.of(
                                DifficultyLevel.BEGINNER, 3,
                                DifficultyLevel.INTERMEDIATE, 4,
                                DifficultyLevel.ADVANCED, 5,
                                DifficultyLevel.ELITE, 6
                        ))
                        .rewardPoints(Map.of(
                                DifficultyLevel.BEGINNER, 150,
                                DifficultyLevel.INTERMEDIATE, 200,
                                DifficultyLevel.ADVANCED, 250,
                                DifficultyLevel.ELITE, 300
                        ))
                        .unlockedQuestId("weekly-champion-quest")
                        .weight(1.0)
                        .build()
        );
    }
}
