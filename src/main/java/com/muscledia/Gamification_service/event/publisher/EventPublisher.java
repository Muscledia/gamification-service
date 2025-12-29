package com.muscledia.Gamification_service.event.publisher;

import com.muscledia.Gamification_service.event.*;

/**
 * PURPOSE: Abstract event publishing without knowing the implementation
 * RESPONSIBILITY: Define event publishing operations
 * COUPLING: None - interface only
 */
public interface EventPublisher {

    void publishChallengeStarted(ChallengeStartedEvent event);
    void publishChallengeProgress(ChallengeProgressEvent event);
    void publishChallengeCompleted(ChallengeCompletedEvent event);

    // Gamification Events
    void publishBadgeEarned(BadgeEarnedEvent event);
    void publishLevelUp(LevelUpEvent event);
    void publishLeaderboardUpdated(LeaderboardUpdatedEvent event);
    void publishStreakUpdated(StreakUpdatedEvent event);
}
