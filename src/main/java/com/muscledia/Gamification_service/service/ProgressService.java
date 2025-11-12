package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.WorkoutCompletedEvent;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PURPOSE: Handle challenge progress updates
 * RESPONSIBILITY: Update progress and handle completions
 * COUPLING: Low - depends only on interfaces
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    private final UserChallengeRepository userChallengeRepository;
    private final ChallengeRepository challengeRepository;
    private final RewardProcessor rewardProcessor;
    private final UserJourneyProfileService userJourneyService;
    private final ChallengeProgressionService challengeProgressionService;

    /**
     * Update challenge progress from workout completion event
     * Called by existing WorkoutEventHandler
     */
    @Transactional
    public void updateChallengeProgress(WorkoutCompletedEvent event) {
        log.info("Updating challenge progress for user {} from workout {}",
                event.getUserId(), event.getWorkoutId());

        List<UserChallenge> activeChallenges = userChallengeRepository
                .findActiveByUserId(event.getUserId());

        if (activeChallenges.isEmpty()) {
            log.debug("No active challenges for user {}", event.getUserId());
            return;
        }

        for (UserChallenge userChallenge : activeChallenges) {
            try {
                updateSingleChallenge(userChallenge, event);
            } catch (Exception e) {
                log.error("Failed to update challenge {} for user {}: {}",
                        userChallenge.getChallengeId(), event.getUserId(), e.getMessage());
            }
        }
    }

    private void updateSingleChallenge(UserChallenge userChallenge, WorkoutCompletedEvent event) {
        Challenge challenge = challengeRepository.findById(userChallenge.getChallengeId())
                .orElse(null);

        if (challenge == null || !challenge.isActive()) {
            return;
        }

        int progressIncrement = calculateProgress(challenge, event);

        if (progressIncrement > 0) {
            boolean wasCompleted = userChallenge.isTargetReached();
            userChallenge.addProgress(progressIncrement);

            log.info("Challenge '{}' progress: +{} (total: {}/{})",
                    challenge.getName(), progressIncrement,
                    userChallenge.getCurrentProgress(), userChallenge.getTargetValue());

            userChallengeRepository.save(userChallenge);

            // Check if just completed
            if (!wasCompleted && userChallenge.isTargetReached()) {
                handleChallengeCompletion(userChallenge, challenge);
            }
        }
    }

    /**
     * Calculate progress increment based on challenge objective and workout data
     * Updated to match your ObjectiveType enum
     */
    private int calculateProgress(Challenge challenge, WorkoutCompletedEvent event) {
        ObjectiveType objective = challenge.getObjectiveType();

        return switch (objective) {
            case REPS -> event.getTotalReps() != null ? event.getTotalReps() : 0;
            case DURATION -> event.getDurationMinutes() != null ? event.getDurationMinutes() : 0;
            case EXERCISES -> event.getExercisesCompleted() != null ? event.getExercisesCompleted() : 0;
            case WEIGHT_LIFTED -> event.getTotalVolume() != null ? event.getTotalVolume().intValue() : 0;
            case TIME_BASED -> 1; // Each workout completion counts as 1
            case ACHIEVEMENT_BASED -> calculateAchievementProgress(event);
            default -> {
                log.warn("Unknown objective type: {}", objective);
                yield 0;
            }
        };
    }

    private int calculateAchievementProgress(WorkoutCompletedEvent event) {
        // Achievement-based challenges (streaks, consistency, etc.)
        return event.isStreakEligible() ? 1 : 0;
    }

    private void handleChallengeCompletion(UserChallenge userChallenge, Challenge challenge) {
        log.info("Challenge '{}' completed by user {}",
                challenge.getName(), userChallenge.getUserId());

        try {
            // TODO Award points
            rewardProcessor.awardPoints(userChallenge.getUserId(), challenge.getRewardPoints());

            // TODO: Unlock quest
            // Unlock quest if specified
            if (challenge.getUnlockedQuestId() != null) {
                rewardProcessor.unlockQuest(userChallenge.getUserId(), challenge.getUnlockedQuestId());
            }

            // NEW: Update user journey progression
            userJourneyService.recordChallengeCompletion(
                    userChallenge.getUserId(), challenge);

            // NEW: Auto-unlock next challenges in progression
            List<Challenge> unlockedChallenges = challengeProgressionService
                    .unlockNextChallenges(userChallenge.getUserId(), challenge);

            if (!unlockedChallenges.isEmpty()) {
                log.info("Unlocked {} new challenges for user {}",
                        unlockedChallenges.size(), userChallenge.getUserId());
            }

            log.info("Challenge rewards processed for user {}: {} points awarded",
                    userChallenge.getUserId(), challenge.getRewardPoints());

        } catch (Exception e) {
            log.error("Failed to process challenge completion rewards for user {}: {}",
                    userChallenge.getUserId(), e.getMessage());
        }
    }
}
