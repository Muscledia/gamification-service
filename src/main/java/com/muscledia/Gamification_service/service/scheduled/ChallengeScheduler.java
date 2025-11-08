package com.muscledia.Gamification_service.service.scheduled;


import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.UserChallengeRepository;
import com.muscledia.Gamification_service.service.GenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * PURPOSE: Automate challenge generation and cleanup
 * RESPONSIBILITY: Run scheduled tasks for challenge lifecycle
 * COUPLING: Low - depends only on service interfaces
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty("gamification.scheduling.enabled")
public class ChallengeScheduler {

    private final GenerationService generationService;
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;

    /**
     * Generate daily challenges at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyChallenges() {
        log.info("Starting daily challenge generation");
        try {
            List<Challenge> generated = generationService.generateDailyChallenges();
            log.info("Successfully generated {} daily challenges", generated.size());
        } catch (Exception e) {
            log.error("Failed to generate daily challenges: {}", e.getMessage());
        }
    }

    /**
     * Generate weekly challenges every Monday at 2:30 AM
     */
    @Scheduled(cron = "0 30 2 ? * MON")
    public void generateWeeklyChallenges() {
        log.info("Starting weekly challenge generation");
        try {
            List<Challenge> generated = generationService.generateWeeklyChallenges();
            log.info("Successfully generated {} weekly challenges", generated.size());
        } catch (Exception e) {
            log.error("Failed to generate weekly challenges: {}", e.getMessage());
        }
    }

    /**
     * Clean up expired challenges at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpired() {
        log.info("Starting expired challenge cleanup");
        try {
            cleanupExpiredUserChallenges();
            cleanupOldChallenges();
        } catch (Exception e) {
            log.error("Failed to cleanup expired challenges: {}", e.getMessage());
        }
    }

    private void cleanupExpiredUserChallenges() {
        List<UserChallenge> expired = userChallengeRepository.findExpiredActive();
        for (UserChallenge userChallenge : expired) {
            userChallenge.setStatus(ChallengeStatus.EXPIRED);
            userChallengeRepository.save(userChallenge);
        }
        log.info("Marked {} user challenges as expired", expired.size());
    }

    private void cleanupOldChallenges() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(30));
        List<Challenge> oldChallenges = challengeRepository.findByEndDateBefore(cutoff);
        challengeRepository.deleteAll(oldChallenges);
        log.info("Deleted {} old challenges", oldChallenges.size());
    }
}
