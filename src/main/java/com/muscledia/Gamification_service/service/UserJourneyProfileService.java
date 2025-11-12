package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.UserJourneyProfile;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.repository.UserJourneyProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserJourneyProfileService {

    private final UserJourneyProfileRepository userJourneyRepository;
    private final UserGamificationService gamificationService;

    public UserJourneyProfile getUserJourney(Long userId) {
        return userJourneyRepository.findByUserId(userId) // FIXED: Now returns Optional
                .orElseGet(() -> createDefaultJourney(userId));
    }

    @Transactional
    public void recordChallengeCompletion(Long userId, Challenge challenge) {
        UserJourneyProfile journey = getUserJourney(userId);

        // Update completion tracking
        if (challenge.getTemplateId() != null) {
            journey.getCompletedChallengeTemplates().add(challenge.getTemplateId());
            journey.incrementChallengeCount(challenge.getTemplateId());
        }

        journey.updateCompletionRate(challenge.getDifficultyLevel());

        // Update preferences based on completed challenge
        journey.addPreferredObjective(challenge.getObjectiveType());
        journey.addJourneyTag(challenge.getUserJourneyTags());

        // Update last completion time
        journey.setLastChallengeCompletedAt(Instant.now());

        // Check for phase progression
        checkPhaseProgression(journey);

        userJourneyRepository.save(journey);
    }

    private void checkPhaseProgression(UserJourneyProfile journey) {
        int completedCount = journey.getCompletedChallengeTemplates().size();
        String currentPhase = journey.getCurrentPhase();

        if ("foundation".equals(currentPhase) && completedCount >= 10) {
            journey.setCurrentPhase("building");
            journey.getActiveJourneyTags().add("intermediate");
            log.info("User {} progressed to building phase", journey.getUserId());
        } else if ("building".equals(currentPhase) && completedCount >= 25) {
            journey.setCurrentPhase("mastery");
            journey.getActiveJourneyTags().add("advanced");
            log.info("User {} progressed to mastery phase", journey.getUserId());
        }
    }

    @Transactional
    public UserJourneyProfile createDefaultJourney(Long userId) {
        UserGamificationProfile profile = gamificationService.getUserProfile(userId);

        UserJourneyProfile journey = UserJourneyProfile.builder()
                .userId(userId)
                .currentPhase("foundation")
                .currentLevel(profile.getLevel())
                .activeJourneyTags(new HashSet<>(Set.of("beginner", "general_fitness")))
                .completedChallengeTemplates(new HashSet<>())
                .templateCompletionCount(new HashMap<>())
                .performanceMetrics(new HashMap<>())
                .preferredObjectives(new HashSet<>())
                .preferredDifficulty(DifficultyLevel.BEGINNER)
                .averageCompletionRate(0.0)
                .consecutiveChallengesCompleted(0)
                .build();

        return userJourneyRepository.save(journey);
    }
}
