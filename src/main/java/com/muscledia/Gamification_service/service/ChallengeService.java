package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.ChallengeStartedEvent;
import com.muscledia.Gamification_service.event.publisher.EventPublisher;
import com.muscledia.Gamification_service.exception.ChallengeAlreadyStartedException;
import com.muscledia.Gamification_service.exception.ChallengeNotActiveException;
import com.muscledia.Gamification_service.exception.ChallengeNotFoundException;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
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

    /**
     * Get available challenges for user based on difficulty
     */
    public List<Challenge> getAvailableChallenges(Long userId, ChallengeType type) {
        log.info("Getting {} challenges for user {}", type, userId);

        DifficultyLevel userDifficulty = determineUserDifficulty(userId);
        List<Challenge> allChallenges = challengeRepository.findByTypeAndActiveTrue(type);

        return allChallenges.stream()
                .filter(challenge -> challenge.getDifficultyLevel() == userDifficulty)
                .filter(challenge -> !userAlreadyStarted(userId, challenge.getId()))
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

    // Private helper methods
    private DifficultyLevel determineUserDifficulty(Long userId) {
        // Simple logic - could be enhanced
        return DifficultyLevel.BEGINNER; // Placeholder
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
