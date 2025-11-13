package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.ChallengeStartedEvent;
import com.muscledia.Gamification_service.event.publisher.EventPublisher;
import com.muscledia.Gamification_service.exception.ChallengeAlreadyStartedException;
import com.muscledia.Gamification_service.exception.ChallengeNotActiveException;
import com.muscledia.Gamification_service.exception.ChallengeNotFoundException;
import com.muscledia.Gamification_service.mapper.ChallengeMapper;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.UserJourneyProfile;
import com.muscledia.Gamification_service.model.UserPerformanceMetrics;
import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;


/**
 * PURPOSE: Core challenge business operations
 * RESPONSIBILITY: Orchestrate challenge lifecycle operations
 * COUPLING: Low - depends only on interfaces
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChallengeService {
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final EventPublisher eventPublisher;
    private final UserJourneyProfileService userJourneyService;
    private final ChallengeProgressionService challengeProgressionService;
    private final UserPerformanceAnalyzer performanceAnalyzer;


    /**
     * ENHANCED: Get challenges based on user progression
     */
    public List<Challenge> getAvailableChallenges(Long userId, ChallengeType type) {
        log.info("Getting {} challenges for user {}", type, userId);

        try {
            UserJourneyProfile userJourney = userJourneyService.getUserJourney(userId);
            List<Challenge> personalizedChallenges = challengeProgressionService
                    .generatePersonalizedChallenges(userId, type, userJourney);

            List<Challenge> availableChallenges = personalizedChallenges.stream()
                    .filter(challenge -> !userAlreadyStarted(userId, challenge.getId()))
                    .limit(5)
                    .collect(Collectors.toList());

            log.debug("Found {} available {} challenges for user {}",
                    availableChallenges.size(), type, userId);
            return availableChallenges;

        } catch (Exception e) {
            log.error("Failed to get available challenges for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve challenges", e);
        }
    }

    /**
     * Start a challenge for user
     */
    public UserChallenge startChallenge(Long userId, String challengeId) {
        log.info("Starting challenge {} for user {}", challengeId, userId);

        try {
            Challenge challenge = challengeRepository.findById(challengeId)
                    .orElseThrow(() -> {
                        log.warn("Challenge not found: {}", challengeId);
                        return new ChallengeNotFoundException("Challenge not found: " + challengeId);
                    });

            log.debug("Found challenge: {} for user {}", challenge.getName(), userId);

            validateCanStart(userId, challenge);

            UserChallenge userChallenge = createUserChallenge(userId, challenge);

            // ENHANCEMENT: Set challenge name and details for better UX
            userChallenge.setChallengeName(challenge.getName());
            userChallenge.setChallengeType(challenge.getType());
            userChallenge.setProgressUnit(ChallengeMapper.getProgressUnit(challenge.getObjectiveType()));

            UserChallenge saved = userChallengeRepository.save(userChallenge);

            log.debug("Created user challenge with ID: {}", saved.getId());

            publishChallengeStartedEvent(saved, challenge);

            log.info("Successfully started challenge {} for user {}", challengeId, userId);
            return saved;

        } catch (ChallengeNotFoundException | ChallengeNotActiveException | ChallengeAlreadyStartedException e) {
            log.warn("Challenge start validation failed for user {}: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to start challenge {} for user {}: {}", challengeId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to start challenge", e);
        }
    }

    /**
     * Get user's active challenges
     */
    public List<UserChallenge> getActiveChallenges(Long userId) {
        log.debug("Getting active challenges for user {}", userId);

        try {
            List<UserChallenge> activeChallenges = userChallengeRepository.findActiveByUserId(userId);
            log.debug("Found {} active challenges for user {}", activeChallenges.size(), userId);
            return activeChallenges;
        } catch (Exception e) {
            log.error("Failed to get active challenges for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve active challenges", e);
        }
    }

    // ENHANCED: Update difficulty determination
    private DifficultyLevel determineUserDifficulty(Long userId) {
        UserJourneyProfile journey = userJourneyService.getUserJourney(userId);
        UserPerformanceMetrics performance = performanceAnalyzer.analyzeUser(userId);

        // Dynamic difficulty based on completion rate and level
        if (performance.getRecentCompletionRate() > 0.8 && journey.getCurrentLevel() > 5) {
            return DifficultyLevel.INTERMEDIATE;
        } else if (performance.getRecentCompletionRate() > 0.9 && journey.getCurrentLevel() > 10) {
            return DifficultyLevel.ADVANCED;
        }
        return DifficultyLevel.BEGINNER;
    }

    private boolean userAlreadyStarted(Long userId, String challengeId) {
        return userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId).isPresent();
    }


    private void validateCanStart(Long userId, Challenge challenge) {
        log.debug("Validating challenge start for user {} and challenge {}", userId, challenge.getId());

        if (!challenge.isActive()) {
            throw new ChallengeNotActiveException("Challenge is not active: " + challenge.getId());
        }

        if (userAlreadyStarted(userId, challenge.getId())) {
            throw new ChallengeAlreadyStartedException("Challenge already started: " + challenge.getId());
        }

        if (challenge.isExpired()) {
            throw new ChallengeNotActiveException("Challenge has expired: " + challenge.getId());
        }
    }

    private UserChallenge createUserChallenge(Long userId, Challenge challenge) {
        return UserChallenge.builder()
                .userId(userId)
                .challengeId(challenge.getId())
                .status(ChallengeStatus.ACTIVE)
                .currentProgress(0)
                .targetValue(challenge.getTargetValue())
                .startedAt(Instant.now())
                .expiresAt(challenge.getEndDate())
                .createdAt(Instant.now())
                .build();
    }

    private void publishChallengeStartedEvent(UserChallenge userChallenge, Challenge challenge) {
        try {
            ChallengeStartedEvent event = ChallengeStartedEvent.of(userChallenge, challenge);
            eventPublisher.publishChallengeStarted(event);
            log.debug("Published challenge started event for user {}", userChallenge.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish challenge started event: {}", e.getMessage());
            // Don't fail the whole operation for event publishing issues
        }
    }
}
