package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.ChallengeCompletedEvent;
import com.muscledia.Gamification_service.event.ChallengeProgressEvent;
import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.event.publisher.EventPublisher;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * PURPOSE: Update challenge progress during workouts
 * RESPONSIBILITY: Progress calculation and completion handling
 * COUPLING: Low - focused on progress updates only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeProgressService {

    private final UserChallengeRepository userChallengeRepository;
    private final ChallengeRepository challengeRepository;
    private final EventPublisher eventPublisher;
    private final UserGamificationService gamificationService;
    private final UserJourneyProfileService journeyService;

    /**
     * Update challenge progress based on workout completion
     */
    @Transactional
    public void updateChallengeProgress(Long userId, WorkoutCompletedEvent event) {
        log.debug("Updating challenge progress for user {}", userId);

        List<UserChallenge> activeChallenges = userChallengeRepository.findActiveByUserId(userId);

        if (activeChallenges.isEmpty()) {
            log.debug("No active challenges for user {}", userId);
            return;
        }

        for (UserChallenge userChallenge : activeChallenges) {
            try {
                updateSingleChallenge(userId, userChallenge, event);
            } catch (Exception e) {
                log.error("Failed to update challenge {} for user {}: {}",
                        userChallenge.getChallengeId(), userId, e.getMessage());
            }
        }
    }

    /**
     * Update a single challenge based on workout data
     */
    private void updateSingleChallenge(Long userId, UserChallenge userChallenge,
                                       WorkoutCompletedEvent event) {
        Challenge challenge = challengeRepository.findById(userChallenge.getChallengeId())
                .orElse(null);

        if (challenge == null) {
            log.warn("Challenge {} not found", userChallenge.getChallengeId());
            return;
        }

        int progressIncrement = calculateProgressIncrement(challenge, event);

        if (progressIncrement == 0) {
            log.debug("No progress for challenge {} (objective: {})",
                    challenge.getId(), challenge.getObjectiveType());
            return;
        }

        int oldProgress = userChallenge.getCurrentProgress();
        int newProgress = oldProgress + progressIncrement;
        userChallenge.setCurrentProgress(newProgress);
        userChallenge.setLastUpdatedAt(Instant.now());

        if (newProgress >= userChallenge.getTargetValue()) {
            completeChallenge(userId, userChallenge, challenge);
        } else {
            publishProgressEvent(userChallenge, challenge, oldProgress, newProgress);
        }

        userChallengeRepository.save(userChallenge);

        log.info("Updated challenge {} for user {}: {}/{}",
                challenge.getName(), userId, newProgress, userChallenge.getTargetValue());
    }

    /**
     * Calculate progress increment based on challenge objective and workout data
     */
    private int calculateProgressIncrement(Challenge challenge, WorkoutCompletedEvent event) {
        ObjectiveType objective = challenge.getObjectiveType();

        return switch (objective) {
            case EXERCISES -> event.getExercisesCompleted() != null ? event.getExercisesCompleted() : 0;
            case REPS -> event.getTotalReps() != null ? event.getTotalReps() : 0;
            case DURATION -> event.getDurationMinutes() != null ? event.getDurationMinutes() : 0;
            case TIME_BASED -> 1;
            case ACHIEVEMENT_BASED -> event.isStreakEligible() ? 1 : 0;
            case VOLUME_BASED -> event.getTotalVolume() != null ? event.getTotalVolume().intValue() : 0;
            case CALORIES -> event.getCaloriesBurned() != null ? event.getCaloriesBurned() : 0;
            case PERSONAL_RECORDS -> event.getPersonalRecordsAchieved() != null ? event.getPersonalRecordsAchieved() : 0;
            default -> {
                log.warn("Unsupported objective type: {}", objective);
                yield 0;
            }
        };
    }

    /**
     * Complete a challenge
     */
    private void completeChallenge(Long userId, UserChallenge userChallenge, Challenge challenge) {
        userChallenge.setStatus(ChallengeStatus.COMPLETED);
        userChallenge.setCompletedAt(Instant.now());

        if (challenge.getRewardPoints() != null && challenge.getRewardPoints() > 0) {
            gamificationService.updateUserPoints(userId, challenge.getRewardPoints());
        }

        journeyService.recordChallengeCompletion(userId, challenge);
        publishCompletionEvent(userChallenge, challenge);

        log.info("🎉 User {} completed challenge: {} ({})",
                userId, challenge.getName(), challenge.getId());
    }

    /**
     * Publish challenge progress event
     */
    private void publishProgressEvent(UserChallenge userChallenge, Challenge challenge,
                                      int oldProgress, int newProgress) {
        try {
            ChallengeProgressEvent event = ChallengeProgressEvent.builder()
                    .userId(userChallenge.getUserId())
                    .challengeId(challenge.getId())
                    .challengeName(challenge.getName())
                    .challengeType(challenge.getType().name())
                    .currentProgress(newProgress)
                    .targetValue(userChallenge.getTargetValue())
                    .previousProgress(oldProgress)
                    .progressIncrement(newProgress - oldProgress)
                    .progressPercentage((double) newProgress / userChallenge.getTargetValue() * 100)
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishChallengeProgress(event);
        } catch (Exception e) {
            log.error("Failed to publish progress event: {}", e.getMessage());
        }
    }

    /**
     * Publish challenge completion event
     */
    private void publishCompletionEvent(UserChallenge userChallenge, Challenge challenge) {
        try {
            ChallengeCompletedEvent event = ChallengeCompletedEvent.builder()
                    .userId(userChallenge.getUserId())
                    .challengeId(challenge.getId())
                    .challengeName(challenge.getName())
                    .challengeType(challenge.getType().name())
                    .finalProgress(userChallenge.getCurrentProgress())
                    .targetValue(userChallenge.getTargetValue())
                    .pointsAwarded(challenge.getRewardPoints())
                    .completedAt(userChallenge.getCompletedAt())
                    .timeTakenHours((int) java.time.Duration.between(
                            userChallenge.getStartedAt(),
                            userChallenge.getCompletedAt()
                    ).toHours())
                    .timestamp(Instant.now())
                    .build();

            eventPublisher.publishChallengeCompleted(event);
        } catch (Exception e) {
            log.error("Failed to publish completion event: {}", e.getMessage());
        }
    }

    /**
     * Expire old active challenges
     */
    @Transactional
    public void expireOldChallenges() {
        Instant now = Instant.now();
        List<UserChallenge> expiredChallenges = userChallengeRepository
                .findExpiredActiveChallenges(now);

        for (UserChallenge challenge : expiredChallenges) {
            challenge.setStatus(ChallengeStatus.EXPIRED);
            challenge.setLastUpdatedAt(now);
            userChallengeRepository.save(challenge);

            log.info("Expired challenge {} for user {}",
                    challenge.getChallengeId(), challenge.getUserId());
        }

        if (!expiredChallenges.isEmpty()) {
            log.info("Expired {} challenges", expiredChallenges.size());
        }
    }
}