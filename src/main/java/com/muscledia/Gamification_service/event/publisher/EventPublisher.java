package com.muscledia.Gamification_service.event.publisher;

import com.muscledia.Gamification_service.event.ChallengeCompletedEvent;
import com.muscledia.Gamification_service.event.ChallengeProgressEvent;
import com.muscledia.Gamification_service.event.ChallengeStartedEvent;

/**
 * PURPOSE: Abstract event publishing without knowing the implementation
 * RESPONSIBILITY: Define event publishing operations
 * COUPLING: None - interface only
 */
public interface EventPublisher {

    void publishChallengeStarted(ChallengeStartedEvent event);
    void publishChallengeProgress(ChallengeProgressEvent event);
    void publishChallengeCompleted(ChallengeCompletedEvent event);
}
