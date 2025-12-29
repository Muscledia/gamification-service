package com.muscledia.Gamification_service.event.publisher;


import com.muscledia.Gamification_service.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * PURPOSE: Simple event publisher implementation (no Kafka/Outbox dependency)
 * RESPONSIBILITY: Log events when full event processing is disabled
 * COUPLING: None - standalone logging implementation
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "gamification.events.processing.enabled", havingValue = "false", matchIfMissing = true)
public class SimpleEventPublisher implements EventPublisher {
    @Override
    public void publishChallengeStarted(ChallengeStartedEvent event) {
        log.info("CHALLENGE_STARTED: User {} started challenge '{}' ({})",
                event.getUserId(), event.getChallengeName(), event.getChallengeId());
    }

    @Override
    public void publishChallengeProgress(ChallengeProgressEvent event) {
        log.info("CHALLENGE_PROGRESS: User {} made progress on challenge '{}': {}/{} ({}%)",
                event.getUserId(), event.getChallengeId(),
                event.getCurrentProgress(), event.getTargetValue(),
                String.format("%.1f", event.getProgressPercentage()));
    }

    @Override
    public void publishChallengeCompleted(ChallengeCompletedEvent event) {
        log.info("CHALLENGE_COMPLETED: User {} completed challenge '{}' and earned {} points",
                event.getUserId(), event.getChallengeName(), event.getPointsAwarded());
    }

    @Override
    public void publishBadgeEarned(BadgeEarnedEvent event) {
        log.info("🎖️ BADGE_EARNED: User {} earned badge '{}' ({})",
                event.getUserId(), event.getBadgeName(), event.getBadgeId());

        if (event.isRareBadge()) {
            log.info("   ⭐ RARE BADGE! Rarity: {}", event.getRarity());
        }
    }

    @Override
    public void publishLevelUp(LevelUpEvent event) {
        log.info("⬆️ LEVEL_UP: User {} leveled up {} → {} ({} total points)",
                event.getUserId(),
                event.getPreviousLevel(),
                event.getNewLevel(),
                event.getTotalPoints());

        if (event.getLevelsGained() > 1) {
            log.info("   🚀 Jumped {} levels!", event.getLevelsGained());
        }

        if (event.isMilestoneLevel()) {
            log.info("   🎯 MILESTONE LEVEL! Significance: {}",
                    event.getLevelUpSignificance());
        }
    }

    @Override
    public void publishLeaderboardUpdated(LeaderboardUpdatedEvent event) {
        String direction = event.getNewRank() < event.getPreviousRank() ? "⬆️" : "⬇️";

        log.info("{} LEADERBOARD_UPDATED: User {} {} from rank {} to rank {} ({})",
                direction,
                event.getUserId(),
                event.getChangeType(),
                event.getPreviousRank(),
                event.getNewRank(),
                event.getLeaderboardType());

        if (event.getNewRank() <= 10) {
            log.info("   🌟 TOP 10 POSITION!");
        }

        if ("TOP_100_ENTRY".equals(event.getChangeType())) {
            log.info("   🎊 First time in TOP 100!");
        }
    }

    @Override
    public void publishStreakUpdated(StreakUpdatedEvent event) {
        String emoji = switch (event.getStreakAction()) {
            case "INCREASED" -> "🔥";
            case "RESET" -> "💔";
            case "MAINTAINED" -> "✨";
            default -> "📈";
        };

        log.info("{} STREAK_UPDATED: User {} {} streak {} ({} → {} days)",
                emoji,
                event.getUserId(),
                event.getStreakType(),
                event.getStreakAction(),
                event.getPreviousStreak(),
                event.getCurrentStreak());

        if (event.isMilestone()) {
            log.info("   🎯 STREAK MILESTONE: {} days! Significance: {}",
                    event.getCurrentStreak(), event.getSignificanceLevel());
        }

        if (event.isNewRecord()) {
            log.info("   🏅 NEW PERSONAL RECORD!");
        }
    }

}
