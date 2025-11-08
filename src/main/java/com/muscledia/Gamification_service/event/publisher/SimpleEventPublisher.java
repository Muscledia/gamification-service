package com.muscledia.Gamification_service.event.publisher;


import com.muscledia.Gamification_service.event.ChallengeCompletedEvent;
import com.muscledia.Gamification_service.event.ChallengeProgressEvent;
import com.muscledia.Gamification_service.event.ChallengeStartedEvent;
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

}
