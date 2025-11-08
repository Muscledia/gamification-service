package com.muscledia.Gamification_service.service;


/**
 * PURPOSE: Abstract reward processing logic
 * RESPONSIBILITY: Define reward operations without implementation details
 * COUPLING: None - interface only
 */
public interface RewardProcessor {

    void awardPoints(Long userId, int points);
    void unlockQuest(Long userId, String questId);
    void awardBadge(Long userId, String badgeId);
}
