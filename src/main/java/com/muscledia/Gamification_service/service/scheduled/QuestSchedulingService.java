package com.muscledia.Gamification_service.service.scheduled;

import com.muscledia.Gamification_service.model.Quest;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.model.enums.QuestType;
import com.muscledia.Gamification_service.repository.QuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Scheduled service for quest management automation.
 * Handles daily/weekly quest generation and expired quest cleanup.
 * 
 * Senior Engineering Note: This service automates quest lifecycle management,
 * ensuring users always have fresh challenges while cleaning up expired
 * content.
 * Uses configurable CRON expressions and batch processing for efficiency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class QuestSchedulingService {

    private final QuestRepository questRepository;

    /**
     * Generate daily quests every day at 2 AM
     */
    @Scheduled(cron = "${gamification.scheduling.quest-generation.cron:0 0 2 * * ?}")
    @Transactional
    public void generateDailyQuests() {
        log.info("Starting daily quest generation");

        try {
            List<Quest> dailyQuests = createDailyQuestTemplates();
            int created = 0;

            for (Quest quest : dailyQuests) {
                try {
                    Quest savedQuest = questRepository.save(quest);
                    created++;
                    log.debug("Created daily quest: {}", savedQuest.getName());
                } catch (Exception e) {
                    log.error("Failed to create daily quest {}: {}", quest.getName(), e.getMessage());
                }
            }

            log.info("Daily quest generation completed. Created {} quests", created);

        } catch (Exception e) {
            log.error("Error during daily quest generation: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate weekly quests every Monday at 2:30 AM
     */
    @Scheduled(cron = "0 30 2 ? * MON")
    @Transactional
    public void generateWeeklyQuests() {
        log.info("Starting weekly quest generation");

        try {
            List<Quest> weeklyQuests = createWeeklyQuestTemplates();
            int created = 0;

            for (Quest quest : weeklyQuests) {
                try {
                    Quest savedQuest = questRepository.save(quest);
                    created++;
                    log.debug("Created weekly quest: {}", savedQuest.getName());
                } catch (Exception e) {
                    log.error("Failed to create weekly quest {}: {}", quest.getName(), e.getMessage());
                }
            }

            log.info("Weekly quest generation completed. Created {} quests", created);

        } catch (Exception e) {
            log.error("Error during weekly quest generation: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up expired quests every day at 3 AM
     */
    @Scheduled(cron = "${gamification.scheduling.expired-quest-cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupExpiredQuests() {
        log.info("Starting expired quest cleanup");

        try {
            Instant now = Instant.now();
            List<Quest> expiredQuests = questRepository.findExpiredQuests(now);

            int processed = 0;
            int deleted = 0;

            for (Quest quest : expiredQuests) {
                try {
                    // Process any final quest state before deletion
                    processExpiredQuest(quest);

                    // Delete old quests (older than 30 days)
                    if (quest.getEndDate().isBefore(now.minus(30, ChronoUnit.DAYS))) {
                        questRepository.delete(quest);
                        deleted++;
                        log.debug("Deleted expired quest: {}", quest.getName());
                    }

                    processed++;

                } catch (Exception e) {
                    log.error("Failed to process expired quest {}: {}", quest.getId(), e.getMessage());
                }
            }

            log.info("Expired quest cleanup completed. Processed: {}, Deleted: {}", processed, deleted);

        } catch (Exception e) {
            log.error("Error during expired quest cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Refresh quest difficulty based on user completion rates (weekly)
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    @Transactional
    public void refreshQuestDifficulty() {
        log.info("Starting quest difficulty refresh");

        try {
            // Analyze completion rates and adjust future quest difficulty
            analyzeQuestPerformance();

            log.info("Quest difficulty refresh completed");

        } catch (Exception e) {
            log.error("Error during quest difficulty refresh: {}", e.getMessage(), e);
        }
    }

    // ===============================
    // QUEST GENERATION METHODS
    // ===============================

    private List<Quest> createDailyQuestTemplates() {
        List<Quest> quests = new ArrayList<>();
        Instant now = Instant.now();
        Instant endOfDay = now.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS);

        // Workout completion quest
        quests.add(createQuest(
                "Daily Workout Challenge",
                "Complete 1 workout session",
                QuestType.DAILY,
                ObjectiveType.EXERCISES,
                1,
                100, // points
                50, // exp
                1, // required level
                now,
                endOfDay));

        // Exercise variety quest
        quests.add(createQuest(
                "Exercise Variety",
                "Complete 3 different exercises",
                QuestType.DAILY,
                ObjectiveType.EXERCISES,
                3,
                75,
                40,
                1,
                now,
                endOfDay));

        // Duration-based quest
        quests.add(createQuest(
                "Active Hour",
                "Exercise for 60 minutes total",
                QuestType.DAILY,
                ObjectiveType.TIME_BASED,
                60,
                150,
                75,
                2,
                now,
                endOfDay));

        return quests;
    }

    private List<Quest> createWeeklyQuestTemplates() {
        List<Quest> quests = new ArrayList<>();
        Instant now = Instant.now();
        Instant endOfWeek = now.plus(7, ChronoUnit.DAYS);

        // Weekly workout consistency
        quests.add(createQuest(
                "Weekly Warrior",
                "Complete 5 workouts this week",
                QuestType.WEEKLY,
                ObjectiveType.EXERCISES,
                5,
                500,
                250,
                3,
                now,
                endOfWeek));

        // Volume-based weekly quest
        quests.add(createQuest(
                "Volume Challenge",
                "Complete 50 total exercises this week",
                QuestType.WEEKLY,
                ObjectiveType.EXERCISES,
                50,
                750,
                400,
                5,
                now,
                endOfWeek));

        // Streak maintenance quest
        quests.add(createQuest(
                "Streak Master",
                "Maintain a 7-day workout streak",
                QuestType.WEEKLY,
                ObjectiveType.ACHIEVEMENT_BASED,
                7,
                1000,
                500,
                10,
                now,
                endOfWeek));

        return quests;
    }

    private Quest createQuest(String name, String description, QuestType type,
            ObjectiveType objectiveType, int targetValue,
            int pointsReward, int expReward, int requiredLevel,
            Instant startDate, Instant endDate) {

        Quest quest = new Quest();
        quest.setName(name);
        quest.setDescription(description);
        quest.setQuestType(type);
        quest.setObjectiveType(objectiveType);
        quest.setObjectiveTarget(targetValue); // Fixed: was setTargetValue
        quest.setPointsReward(pointsReward);
        quest.setExpReward(expReward);
        quest.setRequiredLevel(requiredLevel);
        quest.setStartDate(startDate);
        quest.setEndDate(endDate);
        quest.setRepeatable(false);
        quest.setCreatedAt(Instant.now());

        return quest;
    }

    // ===============================
    // QUEST PROCESSING METHODS
    // ===============================

    private void processExpiredQuest(Quest quest) {
        // Process expired quest logic here
        // Note: Quest model doesn't have isActive/setActive methods
        // This is just cleanup logic for expired quests

        log.debug("Processed expired quest: {}", quest.getName());
    }

    private void analyzeQuestPerformance() {
        try {
            // Get quest completion statistics
            Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

            // Analyze completion rates by quest type and difficulty
            // This is a simplified version - in production you'd analyze user progress data

            List<Quest> recentQuests = questRepository.findQuestsCreatedAfter(oneWeekAgo);

            Map<QuestType, Integer> completionRates = new HashMap<>();

            for (Quest quest : recentQuests) {
                // In a real implementation, you'd calculate actual completion rates
                // based on user progress data
                QuestType type = quest.getQuestType();
                completionRates.put(type, completionRates.getOrDefault(type, 0) + 1);
            }

            log.info("Quest performance analysis completed. Types analyzed: {}", completionRates.keySet());

        } catch (Exception e) {
            log.error("Error analyzing quest performance: {}", e.getMessage());
        }
    }
}