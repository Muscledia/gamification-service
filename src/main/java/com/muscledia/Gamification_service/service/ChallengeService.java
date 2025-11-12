package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.ChallengeStartedEvent;
import com.muscledia.Gamification_service.event.publisher.EventPublisher;
import com.muscledia.Gamification_service.exception.ChallengeAlreadyStartedException;
import com.muscledia.Gamification_service.exception.ChallengeNotActiveException;
import com.muscledia.Gamification_service.exception.ChallengeNotFoundException;
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
    // NEW DEPENDENCIES
    private final UserJourneyProfileService userJourneyService;
    private final ChallengeProgressionService challengeProgressionService;
    private final UserPerformanceAnalyzer performanceAnalyzer;


    /**
     * ENHANCED: Get challenges based on user progression
     */
    public List<Challenge> getAvailableChallenges(Long userId, ChallengeType type) {
        log.info("Getting {} challenges for user {}", type, userId);

        // NEW: Get user's journey profile for personalized challenges
        UserJourneyProfile userJourney = userJourneyService.getUserJourney(userId);

        // NEW: Generate personalized challenges instead of just filtering by difficulty
        List<Challenge> personalizedChallenges = challengeProgressionService
                .generatePersonalizedChallenges(userId, type, userJourney);

        return personalizedChallenges.stream()
                .filter(challenge -> !userAlreadyStarted(userId, challenge.getId()))
                .limit(5) // Don't overwhelm user
                .collect(Collectors.toList());
    }

    /**
     * Start a challenge for user
     */
    public UserChallenge startChallenge(Long userId, String challengeId) {
        log.info("Starting challenge {} for user {}", challengeId, userId);

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeId));

        validateCanStart(userId, challenge);

        UserChallenge userChallenge = createUserChallenge(userId, challenge);
        UserChallenge saved = userChallengeRepository.save(userChallenge);

        // Publish event (loosely coupled)
        eventPublisher.publishChallengeStarted(
                ChallengeStartedEvent.of(saved, challenge));

        return saved;
    }

    /**
     * Get user's active challenges
     */
    public List<UserChallenge> getActiveChallenges(Long userId) {
        return userChallengeRepository.findActiveByUserId(userId);
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
        return userChallengeRepository
                .findByUserIdAndChallengeId(userId, challengeId)
                .isPresent();
    }

    private void validateCanStart(Long userId, Challenge challenge) {
        if (!challenge.isActive()) {
            throw new ChallengeNotActiveException(challenge.getId());
        }

        if (userAlreadyStarted(userId, challenge.getId())) {
            throw new ChallengeAlreadyStartedException(challenge.getId());
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
                .build();
    }
}
