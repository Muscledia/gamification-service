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

/**
 * PURPOSE: Generate personalized challenges based on user progression
 * RESPONSIBILITY: Challenge generation and unlock logic
 * COUPLING: Low - focused on challenge creation only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeProgressionService {

    private final ChallengeTemplateRepository templateRepository;
    private final ChallengeRepository challengeRepository;
    private final UserPerformanceAnalyzer performanceAnalyzer;
    private final UserJourneyProfileService userJourneyProfileService;

    /**
     * Generate personalized challenges for user based on their journey and performance
     */
    public List<Challenge> generatePersonalizedChallenges(Long userId, ChallengeType type,
                                                          UserJourneyProfile userJourney) {
        log.debug("Generating {} challenges for user {} in phase {}",
                type, userId, userJourney.getCurrentPhase());

        List<ChallengeTemplate> eligibleTemplates = templateRepository
                .findByTypeAndPhase(type, userJourney.getCurrentPhase());

        if (eligibleTemplates.isEmpty()) {
            log.warn("No challenge templates found for type {} and phase {}",
                    type, userJourney.getCurrentPhase());
            return Collections.emptyList();
        }

        UserPerformanceMetrics performance = performanceAnalyzer.analyzeUser(userId);
        List<Challenge> generatedChallenges = new ArrayList<>();

        for (ChallengeTemplate template : eligibleTemplates) {
            if (isEligibleForUser(template, userJourney)) {
                Challenge existingChallenge = findExistingChallenge(template, type);

                if (existingChallenge != null) {
                    generatedChallenges.add(existingChallenge);
                    log.debug("Reusing existing challenge: {}", existingChallenge.getName());
                } else {
                    Challenge newChallenge = createPersonalizedChallenge(template, userJourney, performance);
                    Challenge savedChallenge = challengeRepository.save(newChallenge);
                    generatedChallenges.add(savedChallenge);

                    log.info("Generated new challenge: {} ({}) for user {}",
                            savedChallenge.getName(), savedChallenge.getId(), userId);
                }
            }
        }

        log.info("Generated {} {} challenges for user {}",
                generatedChallenges.size(), type, userId);

        return generatedChallenges;
    }

    /**
     * Unlock next challenges in progression chain
     */
    public List<Challenge> unlockNextChallenges(Long userId, Challenge completedChallenge) {
        if (completedChallenge.getUnlocksChallengeIds() == null ||
                completedChallenge.getUnlocksChallengeIds().isEmpty()) {
            return Collections.emptyList();
        }

        UserJourneyProfile journey = userJourneyProfileService.getUserJourney(userId);
        UserPerformanceMetrics performance = performanceAnalyzer.analyzeUser(userId);
        List<Challenge> unlockedChallenges = new ArrayList<>();

        for (String templateId : completedChallenge.getUnlocksChallengeIds()) {
            ChallengeTemplate template = templateRepository.findById(templateId).orElse(null);

            if (template != null && isEligibleForUser(template, journey)) {
                Challenge newChallenge = createPersonalizedChallenge(template, journey, performance);
                Challenge saved = challengeRepository.save(newChallenge);
                unlockedChallenges.add(saved);

                log.info("Unlocked new challenge: {} for user {}", saved.getName(), userId);
            }
        }

        return unlockedChallenges;
    }

    private Challenge findExistingChallenge(ChallengeTemplate template, ChallengeType type) {
        Instant now = Instant.now();
        Instant periodStart = calculatePeriodStart(now, type);
        Instant periodEnd = calculatePeriodEnd(now, type);

        return challengeRepository.findByTemplateIdAndDateRange(
                        template.getId(), periodStart, periodEnd)
                .orElse(null);
    }

    private Instant calculatePeriodStart(Instant now, ChallengeType type) {
        return switch (type) {
            case DAILY -> now.truncatedTo(ChronoUnit.DAYS);
            case WEEKLY -> {
                int dayOfWeek = now.atZone(java.time.ZoneOffset.UTC).getDayOfWeek().getValue();
                yield now.minus(Duration.ofDays(dayOfWeek - 1)).truncatedTo(ChronoUnit.DAYS);
            }
            case MONTHLY -> now.atZone(java.time.ZoneOffset.UTC)
                    .withDayOfMonth(1).toInstant().truncatedTo(ChronoUnit.DAYS);
            case YEARLY -> now.atZone(java.time.ZoneOffset.UTC)
                    .withDayOfYear(1).toInstant().truncatedTo(ChronoUnit.DAYS);
        };
    }

    private Instant calculatePeriodEnd(Instant start, ChallengeType type) {
        return switch (type) {
            case DAILY -> start.plus(Duration.ofDays(1));
            case WEEKLY -> start.plus(Duration.ofDays(7));
            case MONTHLY -> start.plus(Duration.ofDays(30));
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
        Instant endDate = calculateEndDate(startDate, template.getType());

        return Challenge.builder()
                .templateId(template.getId())
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

    private Instant calculateEndDate(Instant startDate, ChallengeType type) {
        return switch (type) {
            case DAILY -> startDate.plus(Duration.ofDays(1)).minus(Duration.ofSeconds(1));
            case WEEKLY -> startDate.plus(Duration.ofDays(7)).minus(Duration.ofSeconds(1));
            case MONTHLY -> startDate.plus(Duration.ofDays(30)).minus(Duration.ofSeconds(1));
            case YEARLY -> startDate.plus(Duration.ofDays(365)).minus(Duration.ofSeconds(1));
        };
    }

    private double calculateDifficultyMultiplier(UserPerformanceMetrics performance) {
        double baseMultiplier = 1.0;

        if (performance.getRecentCompletionRate() > 0.8) {
            baseMultiplier *= 1.2;
        } else if (performance.getRecentCompletionRate() < 0.4) {
            baseMultiplier *= 0.8;
        }

        return Math.max(0.5, Math.min(2.0, baseMultiplier));
    }

    private boolean isEligibleForUser(ChallengeTemplate template, UserJourneyProfile userJourney) {
        if (template.getPrerequisiteTemplates() != null &&
                !template.getPrerequisiteTemplates().isEmpty()) {

            boolean hasPrerequisites = template.getPrerequisiteTemplates().stream()
                    .allMatch(userJourney::hasCompletedChallenge);

            if (!hasPrerequisites) {
                log.debug("User {} missing prerequisites for template {}",
                        userJourney.getUserId(), template.getId());
                return false;
            }
        }

        if (template.getJourneyPhase() != null &&
                !template.getJourneyPhase().equals(userJourney.getCurrentPhase())) {
            log.debug("Template {} phase {} doesn't match user phase {}",
                    template.getId(), template.getJourneyPhase(), userJourney.getCurrentPhase());
            return false;
        }

        return true;
    }

    private DifficultyLevel calculateAdjustedDifficulty(DifficultyLevel preferredDifficulty,
                                                        UserPerformanceMetrics performance) {
        if (performance.isPerformingWell() && preferredDifficulty != DifficultyLevel.ELITE) {
            DifficultyLevel[] levels = DifficultyLevel.values();
            int currentIndex = preferredDifficulty.ordinal();
            return levels[Math.min(currentIndex + 1, levels.length - 1)];
        } else if (performance.needsEasierChallenges() && preferredDifficulty != DifficultyLevel.BEGINNER) {
            DifficultyLevel[] levels = DifficultyLevel.values();
            int currentIndex = preferredDifficulty.ordinal();
            return levels[Math.max(currentIndex - 1, 0)];
        }

        return preferredDifficulty;
    }
}