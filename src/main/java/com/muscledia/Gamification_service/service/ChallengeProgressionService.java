package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.ChallengeTemplate;
import com.muscledia.Gamification_service.model.UserJourneyProfile;
import com.muscledia.Gamification_service.model.UserPerformanceMetrics;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.ChallengeTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeProgressionService {

    private final ChallengeTemplateRepository templateRepository;
    private final ChallengeRepository challengeRepository;
    private final UserPerformanceAnalyzer performanceAnalyzer;
    private final UserJourneyProfileService userJourneyProfileService;

    public List<Challenge> generatePersonalizedChallenges(Long userId, ChallengeType type,
                                                          UserJourneyProfile userJourney) {

        // Get eligible templates
        List<ChallengeTemplate> eligibleTemplates = templateRepository
                .findByTypeAndPhase(type, userJourney.getCurrentPhase());

        UserPerformanceMetrics performance = performanceAnalyzer.analyzeUser(userId);

        List<Challenge> generatedChallenges = new ArrayList<>();

        for (ChallengeTemplate template : eligibleTemplates) {
            if (isEligibleForUser(template, userJourney)) {

                // Check if challenge already exists for today/this period
                Challenge existingChallenge = findExistingChallenge(template, type);

                if (existingChallenge != null) {
                    generatedChallenges.add(existingChallenge);
                } else {
                    // Create and save new challenge
                    Challenge newChallenge = createPersonalizedChallenge(template, userJourney, performance);
                    Challenge savedChallenge = challengeRepository.save(newChallenge); // SAVE TO GET ID
                    generatedChallenges.add(savedChallenge);

                    log.debug("Generated new challenge: {} ({})", savedChallenge.getName(), savedChallenge.getId());
                }
            }
        }

        return generatedChallenges;
    }

    // ADD THIS METHOD: Check if challenge already exists for current period
    private Challenge findExistingChallenge(ChallengeTemplate template, ChallengeType type) {
        Instant now = Instant.now();
        Instant periodStart = calculatePeriodStart(now, type);
        Instant periodEnd = calculatePeriodEnd(now, type);

        // Find existing challenge for this template and time period
        return challengeRepository.findByTemplateIdAndDateRange(
                        template.getId(), periodStart, periodEnd)
                .orElse(null);
    }

    // ADD THESE HELPER METHODS
    private Instant calculatePeriodStart(Instant now, ChallengeType type) {
        return switch (type) {
            case DAILY -> now.truncatedTo(ChronoUnit.DAYS);
            case WEEKLY -> now.minus(Duration.ofDays(now.atZone(java.time.ZoneOffset.UTC).getDayOfWeek().getValue() - 1))
                    .truncatedTo(ChronoUnit.DAYS);
            case MONTHLY -> now.atZone(java.time.ZoneOffset.UTC).withDayOfMonth(1).toInstant()
                    .truncatedTo(ChronoUnit.DAYS);
            case YEARLY -> now.atZone(java.time.ZoneOffset.UTC).withDayOfYear(1).toInstant()
                    .truncatedTo(ChronoUnit.DAYS);
        };
    }

    private Instant calculatePeriodEnd(Instant start, ChallengeType type) {
        return switch (type) {
            case DAILY -> start.plus(Duration.ofDays(1));
            case WEEKLY -> start.plus(Duration.ofDays(7));
            case MONTHLY -> start.plus(Duration.ofDays(30)); // Approximate
            case YEARLY -> start.plus(Duration.ofDays(365));
        };
    }

    private Challenge createPersonalizedChallenge(ChallengeTemplate template,
                                                  UserJourneyProfile journey,
                                                  UserPerformanceMetrics performance) {

        double difficultyMultiplier = calculateDifficultyMultiplier(performance);
        DifficultyLevel adjustedDifficulty = calculateAdjustedDifficulty(
                journey.getPreferredDifficulty(), performance);

        Instant startDate = Instant.now();
        Instant endDate = template.getType().calculateExpiryTime(startDate);

        return Challenge.builder()
                .templateId(template.getId()) // Link to template
                .name(template.getName())
                .description(template.getDescription())
                .type(template.getType())
                .objectiveType(template.getObjective())
                .targetValue((int)(template.getTargetValue(adjustedDifficulty) * difficultyMultiplier))
                .rewardPoints(template.getRewardPoints(adjustedDifficulty))
                .difficultyLevel(adjustedDifficulty)
                .prerequisiteChallengeIds(template.getPrerequisiteTemplates())
                .unlocksChallengeIds(template.getUnlocksTemplates())
                .userJourneyTags(new HashSet<>(template.getUserJourneyTags()))
                .journeyPhase(journey.getCurrentPhase())
                .personalizedDifficultyMultiplier(difficultyMultiplier)
                .startDate(startDate)
                .endDate(endDate)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    public List<Challenge> unlockNextChallenges(Long userId, Challenge completedChallenge) {
        if (completedChallenge.getUnlocksChallengeIds() == null) {
            return Collections.emptyList();
        }

        UserJourneyProfile journey = userJourneyProfileService.getUserJourney(userId);
        List<Challenge> unlockedChallenges = new ArrayList<>();

        for (String templateId : completedChallenge.getUnlocksChallengeIds()) {
            ChallengeTemplate template = templateRepository.findById(templateId).orElse(null);
            if (template != null && isEligibleForUser(template, journey)) {

                Challenge newChallenge = createPersonalizedChallenge(template, journey,
                        performanceAnalyzer.analyzeUser(userId));

                Challenge saved = challengeRepository.save(newChallenge);
                unlockedChallenges.add(saved);
            }
        }

        return unlockedChallenges;
    }


    private double calculateDifficultyMultiplier(UserPerformanceMetrics performance) {
        double baseMultiplier = 1.0;

        // Increase difficulty if user is completing challenges too easily
        if (performance.getRecentCompletionRate() > 0.8) {
            baseMultiplier *= 1.2;
        } else if (performance.getRecentCompletionRate() < 0.4) {
            baseMultiplier *= 0.8;
        }

        return Math.max(0.5, Math.min(2.0, baseMultiplier));
    }

    private boolean isEligibleForUser(ChallengeTemplate template, UserJourneyProfile userJourney) {
        // Check if user has completed prerequisites
        if (template.getPrerequisiteTemplates() != null && !template.getPrerequisiteTemplates().isEmpty()) {
            boolean hasPrerequisites = template.getPrerequisiteTemplates().stream()
                    .allMatch(userJourney::hasCompletedChallenge);
            if (!hasPrerequisites) {
                return false;
            }
        }

        // Check journey phase alignment
        return template.getJourneyPhase() == null ||
                template.getJourneyPhase().equals(userJourney.getCurrentPhase());
    }

    private DifficultyLevel calculateAdjustedDifficulty(DifficultyLevel preferredDifficulty,
                                                        UserPerformanceMetrics performance) {

        if (performance.isPerformingWell() && preferredDifficulty != DifficultyLevel.ELITE) {
            // Move up one difficulty level
            DifficultyLevel[] levels = DifficultyLevel.values();
            int currentIndex = preferredDifficulty.ordinal();
            return levels[Math.min(currentIndex + 1, levels.length - 1)];
        } else if (performance.needsEasierChallenges() && preferredDifficulty != DifficultyLevel.BEGINNER) {
            // Move down one difficulty level
            DifficultyLevel[] levels = DifficultyLevel.values();
            int currentIndex = preferredDifficulty.ordinal();
            return levels[Math.max(currentIndex - 1, 0)];
        }

        return preferredDifficulty;
    }

}
