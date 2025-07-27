package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Quest;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.UserQuestProgress;
import com.muscledia.Gamification_service.model.enums.QuestStatus;
import com.muscledia.Gamification_service.model.enums.QuestType;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.repository.QuestRepository;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class QuestService {

    private final QuestRepository questRepository;
    private final UserGamificationProfileRepository userProfileRepository;

    /**
     * Create a new quest
     */
    @Transactional
    public Quest createQuest(Quest quest) {
        log.info("Creating new quest: {}", quest.getName());

        // Validate quest doesn't already exist
        if (questRepository.findByName(quest.getName()).isPresent()) {
            throw new IllegalArgumentException("Quest with name '" + quest.getName() + "' already exists");
        }

        // Set creation timestamp
        quest.setCreatedAt(Instant.now());

        Quest savedQuest = questRepository.save(quest);
        log.info("Quest created successfully: {}", savedQuest.getId());
        return savedQuest;
    }

    /**
     * Get active quests for a user
     */
    public List<Quest> getActiveQuestsForUser(Long userId) {
        log.info("Getting active quests for user {}", userId);

        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        Instant now = Instant.now();

        // Get all active quests suitable for user level
        List<Quest> activeQuests = questRepository.findActiveQuestsForUserLevel(now, userProfile.getLevel());

        // Filter out already completed non-repeatable quests
        Set<String> completedQuestIds = getUserCompletedQuestIds(userProfile);

        return activeQuests.stream()
                .filter(quest -> quest.isRepeatable() || !completedQuestIds.contains(quest.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get all quests with optional filtering
     */
    public List<Quest> getAllQuests(QuestType questType, ObjectiveType objectiveType, Integer userLevel) {
        Instant now = Instant.now();

        if (questType != null && userLevel != null) {
            return questRepository.findByQuestTypeAndRequiredLevelLessThanEqual(questType, userLevel);
        } else if (questType != null) {
            return questRepository.findByQuestType(questType);
        } else if (objectiveType != null) {
            return questRepository.findByObjectiveType(objectiveType);
        } else if (userLevel != null) {
            return questRepository.findActiveQuestsForUserLevel(now, userLevel);
        } else {
            return questRepository.findActiveQuests(now);
        }
    }

    /**
     * Start a quest for a user
     */
    @Transactional
    public UserQuestProgress startQuest(Long userId, String questId) {
        log.info("Starting quest {} for user {}", questId, userId);

        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found: " + questId));

        // Check if user meets level requirement
        if (quest.getRequiredLevel() > userProfile.getLevel()) {
            throw new IllegalArgumentException(
                    String.format("User level %d is below required level %d for quest %s",
                            userProfile.getLevel(), quest.getRequiredLevel(), quest.getName()));
        }

        List<UserQuestProgress> userQuests = userProfile.getQuests();
        if (userQuests == null) {
            userQuests = new ArrayList<>();
            userProfile.setQuests(userQuests);
        }

        // Check if user already has this quest in progress
        boolean questInProgress = userQuests.stream()
                .anyMatch(questProgress -> questProgress.getQuestId().equals(questId) &&
                        QuestStatus.IN_PROGRESS.equals(questProgress.getStatus()));

        if (questInProgress) {
            throw new IllegalArgumentException("Quest is already in progress for this user");
        }

        // Create new quest progress
        UserQuestProgress questProgress = new UserQuestProgress();
        questProgress.setQuestId(questId);
        questProgress.setObjectiveProgress(0);
        questProgress.setStatus(QuestStatus.IN_PROGRESS);
        questProgress.setStartDate(Instant.now());
        questProgress.setCreatedAt(Instant.now());

        userQuests.add(questProgress);
        userProfileRepository.save(userProfile);

        log.info("Successfully started quest {} for user {}", questId, userId);
        return questProgress;
    }

    /**
     * Update quest progress for a user
     */
    @Transactional
    public UserQuestProgress updateQuestProgress(Long userId, String questId, int progressIncrement) {
        log.info("Updating quest progress for user {} on quest {} with increment {}",
                userId, questId, progressIncrement);

        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found: " + questId));

        List<UserQuestProgress> userQuests = userProfile.getQuests();
        if (userQuests == null) {
            userQuests = new ArrayList<>();
            userProfile.setQuests(userQuests);
        }

        // Find existing quest progress or create new one
        UserQuestProgress questProgress = userQuests.stream()
                .filter(q -> questId.equals(q.getQuestId()))
                .findFirst()
                .orElse(null);

        if (questProgress == null) {
            // Create new quest progress
            questProgress = new UserQuestProgress();
            questProgress.setQuestId(questId);
            questProgress.setObjectiveProgress(0);
            questProgress.setStatus(QuestStatus.IN_PROGRESS);
            questProgress.setStartDate(Instant.now());
            questProgress.setCreatedAt(Instant.now());
            userQuests.add(questProgress);
        }

        // Update progress
        int newProgress = questProgress.getObjectiveProgress() + progressIncrement;
        questProgress.setObjectiveProgress(newProgress);

        // Check if quest is completed
        if (newProgress >= quest.getObjectiveTarget()) {
            questProgress.setStatus(QuestStatus.COMPLETED);
            questProgress.setCompletionDate(Instant.now());

            // Award quest rewards
            awardQuestRewards(userProfile, quest);

            log.info("Quest {} completed by user {}", questId, userId);
        }

        // Save updated profile
        userProfileRepository.save(userProfile);

        return questProgress;
    }

    /**
     * Complete a quest for a user
     */
    @Transactional
    public UserGamificationProfile completeQuest(Long userId, String questId) {
        log.info("Completing quest {} for user {}", questId, userId);

        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found: " + questId));

        // Award experience points
        userProfile.setPoints(userProfile.getPoints() + quest.getPointsReward());

        // Award experience (if you have an experience system)
        // userProfile.setExperience(userProfile.getExperience() +
        // quest.getExpReward());

        // Check for level up
        int newLevel = calculateLevel(userProfile.getPoints());
        if (newLevel > userProfile.getLevel()) {
            userProfile.setLevel(newLevel);
            userProfile.setLastLevelUpDate(Instant.now());
            log.info("User {} leveled up to level {} from quest completion", userId, newLevel);
        }

        // Mark quest as completed (this would be implemented when quest progress is
        // properly modeled)
        // questProgress.setStatus(QuestStatus.COMPLETED);
        // questProgress.setCompletionDate(Instant.now());

        UserGamificationProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("Quest {} completed for user {}", questId, userId);
        return savedProfile;
    }

    /**
     * Get user's quest progress
     */
    public List<UserQuestProgress> getUserQuestProgress(Long userId, QuestStatus status) {
        log.info("Getting quest progress for user {} with status {}", userId, status);

        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        List<UserQuestProgress> allQuests = userProfile.getQuests();

        if (allQuests == null || allQuests.isEmpty()) {
            log.debug("No quest progress found for user {}", userId);
            return new ArrayList<>();
        }

        // Filter by status if provided
        if (status != null) {
            return allQuests.stream()
                    .filter(quest -> status.equals(quest.getStatus()))
                    .toList();
        }

        return new ArrayList<>(allQuests);
    }

    /**
     * Get quests by type and difficulty
     */
    public List<Quest> getQuestsByTypeAndDifficulty(QuestType questType, int maxLevel) {
        return questRepository.findByQuestTypeAndRequiredLevelLessThanEqual(questType, maxLevel);
    }

    /**
     * Get upcoming quests
     */
    public List<Quest> getUpcomingQuests() {
        return questRepository.findUpcomingQuests(Instant.now());
    }

    /**
     * Get expired quests
     */
    public List<Quest> getExpiredQuests() {
        return questRepository.findExpiredQuests(Instant.now());
    }

    /**
     * Get quest statistics
     */
    public Map<String, Object> getQuestStatistics() {
        Map<String, Object> stats = new HashMap<>();

        Instant now = Instant.now();

        long totalQuests = questRepository.count();
        stats.put("totalQuests", totalQuests);

        long activeQuests = questRepository.findActiveQuests(now).size();
        stats.put("activeQuests", activeQuests);

        long upcomingQuests = questRepository.findUpcomingQuests(now).size();
        stats.put("upcomingQuests", upcomingQuests);

        long expiredQuests = questRepository.findExpiredQuests(now).size();
        stats.put("expiredQuests", expiredQuests);

        // Count by quest type
        Map<QuestType, Long> questTypeCount = new HashMap<>();
        for (QuestType type : QuestType.values()) {
            questTypeCount.put(type, (long) questRepository.findByQuestType(type).size());
        }
        stats.put("questsByType", questTypeCount);

        // Count by objective type
        Map<ObjectiveType, Long> objectiveTypeCount = new HashMap<>();
        for (ObjectiveType type : ObjectiveType.values()) {
            objectiveTypeCount.put(type, (long) questRepository.findByObjectiveType(type).size());
        }
        stats.put("questsByObjectiveType", objectiveTypeCount);

        return stats;
    }

    /**
     * Delete a quest
     */
    @Transactional
    public void deleteQuest(String questId) {
        log.info("Deleting quest: {}", questId);

        if (!questRepository.existsById(questId)) {
            throw new IllegalArgumentException("Quest not found: " + questId);
        }

        // TODO: Consider impact on users with active quest progress
        questRepository.deleteById(questId);
        log.info("Quest deleted successfully: {}", questId);
    }

    /**
     * Auto-complete expired quests with progress
     */
    @Transactional
    public void processExpiredQuests() {
        log.info("Processing expired quests");

        List<Quest> expiredQuests = questRepository.findExpiredQuests(Instant.now());

        for (Quest quest : expiredQuests) {
            // This would iterate through users with active progress on this quest
            // and mark them as failed or give partial rewards
            log.info("Processing expired quest: {}", quest.getName());
        }
    }

    /**
     * Generate daily/weekly quests
     */
    @Transactional
    public List<Quest> generateScheduledQuests() {
        log.info("Generating scheduled quests");

        // This would contain logic to create daily/weekly quests
        // based on templates or algorithms

        return questRepository.findScheduledQuests();
    }

    /**
     * Private helper methods
     */
    private Set<String> getUserCompletedQuestIds(UserGamificationProfile userProfile) {
        if (userProfile.getQuests() == null) {
            return new HashSet<>();
        }

        return userProfile.getQuests().stream()
                .filter(quest -> QuestStatus.COMPLETED.equals(quest.getStatus()))
                .map(UserQuestProgress::getQuestId)
                .collect(Collectors.toSet());
    }

    private int calculateLevel(int points) {
        if (points < 100)
            return 1;
        if (points < 300)
            return 2;
        if (points < 600)
            return 3;
        if (points < 1000)
            return 4;
        if (points < 1500)
            return 5;
        if (points < 2100)
            return 6;
        if (points < 2800)
            return 7;
        if (points < 3600)
            return 8;
        if (points < 4500)
            return 9;
        if (points < 5500)
            return 10;

        // Beyond level 10, each level requires 1000 additional points
        return 10 + ((points - 5500) / 1000);
    }

    /**
     * Award quest completion rewards to user
     */
    private void awardQuestRewards(UserGamificationProfile userProfile, Quest quest) {
        // Award points
        int currentPoints = userProfile.getPoints();
        userProfile.setPoints(currentPoints + quest.getPointsReward());

        // Calculate new level
        int newLevel = calculateLevel(userProfile.getPoints());
        if (newLevel > userProfile.getLevel()) {
            userProfile.setLevel(newLevel);
            userProfile.setLastLevelUpDate(Instant.now());
            log.info("User {} leveled up to {} from quest completion",
                    userProfile.getUserId(), newLevel);
        }

        log.info("Awarded {} points to user {} for completing quest {}",
                quest.getPointsReward(), userProfile.getUserId(), quest.getName());
    }
}