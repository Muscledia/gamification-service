package com.muscledia.Gamification_service.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * PURPOSE: Integration with existing reward/gamification systems
 * RESPONSIBILITY: Award points, unlock quests, award badges
 * COUPLING: High to gamification services, Low to domain
 */
@Component
@Slf4j
public class RewardProcessorImpl implements RewardProcessor {
    private final UserGamificationService gamificationService;
    private final QuestService questService;
    private final BadgeService badgeService;

    public RewardProcessorImpl(
            UserGamificationService gamificationService,
            @Lazy QuestService questService,  // This will break the circular dependency
            BadgeService badgeService) {
        this.gamificationService = gamificationService;
        this.questService = questService;
        this.badgeService = badgeService;
    }

    @Override
    public void awardPoints(Long userId, int points) {
        try {
            gamificationService.updateUserPoints(userId, points);
            log.info("Awarded {} points to user {}", points, userId);
        } catch (Exception e) {
            log.error("Failed to award points to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void unlockQuest(Long userId, String questId) {
        try {
            questService.startQuest(userId, questId);
            log.info("Started/unlocked quest {} for user {}", questId, userId);
        } catch (Exception e) {
            log.error("Failed to start quest {} for user {}: {}", questId, userId, e.getMessage());
        }
    }

    @Override
    public void awardBadge(Long userId, String badgeId) {
        try {
            badgeService.awardBadge(userId, badgeId);
            log.info("Awarded badge {} to user {}", badgeId, userId);
        } catch (Exception e) {
            log.error("Failed to award badge {} to user {}: {}", badgeId, userId, e.getMessage());
        }
    }
}
