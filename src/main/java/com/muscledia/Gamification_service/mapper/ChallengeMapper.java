package com.muscledia.Gamification_service.mapper;

import com.muscledia.Gamification_service.dto.request.ChallengeDto;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.ChallengeTemplate;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.ChallengeTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PURPOSE: Convert between Challenge entity and DTO
 * RESPONSIBILITY: Apply Signal vs Noise - only expose what users need
 * COUPLING: Low - depends on repositories for name lookups only
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeMapper {

    private final ChallengeTemplateRepository templateRepository;
    private final ChallengeRepository challengeRepository;

    /**
     * Convert Challenge + UserChallenge to user-friendly DTO
     * SIGNAL ONLY - no technical noise
     */
    public ChallengeDto toDto(Challenge challenge, UserChallenge userChallenge) {
        if (challenge == null) {
            return null;
        }

        ChallengeDto.ChallengeDtoBuilder builder = ChallengeDto.builder()
                // Identity
                .id(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())

                // Classification
                .type(challenge.getType())
                .category(challenge.getCategory())
                .difficultyLevel(challenge.getDifficultyLevel())
                .journeyTags(extractJourneyTags(challenge))        // ← ADD THIS
                .journeyPhase(challenge.getJourneyPhase())

                // Goal
                .targetValue(challenge.getTargetValue())
                .progressUnit(getProgressUnit(challenge.getObjectiveType()))

                // Progress (if user has started)
                .currentProgress(userChallenge != null ? userChallenge.getCurrentProgress() : 0)
                .completionPercentage(calculateCompletionPercentage(userChallenge, challenge))

                // Urgency
                .timeRemaining(calculateTimeRemaining(
                        userChallenge != null ? userChallenge.getExpiresAt() : challenge.getEndDate()
                ))

                // Rewards
                .rewardPoints(challenge.getRewardPoints())
                .rewardCoins(calculateFitnessCoins(challenge.getRewardPoints()))
                .experiencePoints(calculateExperiencePoints(challenge.getRewardPoints()))

                // Motivation
                .isMilestone(isMilestone(challenge))
                .isLegendary(isLegendary(challenge))
                .completionMessage(extractCompletionMessage(challenge))

                // Guidance
                .exerciseFocus(extractExerciseFocus(challenge))
                .safetyNote(extractSafetyNote(challenge))
                .tips(generateTips(challenge))

                // Context (FULLY IMPLEMENTED)
                .prerequisites(extractPrerequisiteNames(challenge))
                .unlocks(extractUnlockNames(challenge));

        return builder.build();
    }
    /**
     * Extract journey tags from challenge
     * Converts Set<String> to List<String> for consistent API
     */
    private List<String> extractJourneyTags(Challenge challenge) {
        if (challenge.getUserJourneyTags() == null || challenge.getUserJourneyTags().isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(challenge.getUserJourneyTags());
    }


    /**
     * Overload for challenges user hasn't started yet
     */
    public ChallengeDto toDto(Challenge challenge) {
        return toDto(challenge, null);
    }

    // ========== SIGNAL GENERATION HELPERS ==========

    /**
     * Convert technical ObjectiveType to user-friendly progress unit
     */
    public static String getProgressUnit(ObjectiveType objectiveType) {
        if (objectiveType == null) return "points";

        return switch (objectiveType) {
            case EXERCISES -> "exercises";
            case REPS -> "reps";
            case DURATION -> "minutes";
            case TIME_BASED -> "workouts";
            case ACHIEVEMENT_BASED -> "achievements";
            case VOLUME_BASED -> "kg";
            case CALORIES -> "calories";
            case PERSONAL_RECORDS -> "PRs";
        };
    }

    /**
     * Calculate human-readable time remaining
     * SIGNAL: "6 hours left" vs NOISE: "2026-01-04T23:59:59Z"
     */
    private String calculateTimeRemaining(Instant expiresAt) {
        if (expiresAt == null) return "No deadline";

        Duration duration = Duration.between(Instant.now(), expiresAt);

        if (duration.isNegative()) {
            return "Expired";
        }

        long hours = duration.toHours();
        if (hours < 1) {
            long minutes = duration.toMinutes();
            return minutes + (minutes == 1 ? " minute left" : " minutes left");
        }

        if (hours < 24) {
            return hours + (hours == 1 ? " hour left" : " hours left");
        }

        long days = duration.toDays();
        return days + (days == 1 ? " day left" : " days left");
    }

    /**
     * Calculate completion percentage
     */
    private Double calculateCompletionPercentage(UserChallenge userChallenge, Challenge challenge) {
        if (userChallenge == null || challenge.getTargetValue() == 0) {
            return 0.0;
        }

        double percentage = (userChallenge.getCurrentProgress().doubleValue() / challenge.getTargetValue()) * 100.0;
        return Math.min(100.0, Math.max(0.0, percentage)); // Clamp 0-100
    }

    /**
     * Calculate fitness coins from points (10 points = 1 coin)
     */
    private Integer calculateFitnessCoins(Integer rewardPoints) {
        return rewardPoints != null ? rewardPoints / 10 : 0;
    }

    /**
     * Calculate experience points from reward points (4 points = 1 XP)
     */
    private Integer calculateExperiencePoints(Integer rewardPoints) {
        return rewardPoints != null ? rewardPoints / 4 : 0;
    }

    /**
     * Check if challenge is a milestone
     */
    private Boolean isMilestone(Challenge challenge) {
        return challenge.getPersonalizationData() != null &&
                Boolean.TRUE.equals(challenge.getPersonalizationData().get("milestone"));
    }

    /**
     * Check if challenge is legendary
     */
    private Boolean isLegendary(Challenge challenge) {
        return challenge.getPersonalizationData() != null &&
                Boolean.TRUE.equals(challenge.getPersonalizationData().get("legendary"));
    }

    /**
     * Extract completion message from metadata
     */
    private String extractCompletionMessage(Challenge challenge) {
        if (challenge.getCompletionMessage() != null) {
            return challenge.getCompletionMessage();
        }

        // Fallback to metadata
        if (challenge.getPersonalizationData() != null) {
            Object message = challenge.getPersonalizationData().get("completionMessage");
            if (message != null) {
                return message.toString();
            }
        }

        // Default motivational message
        return "Great job completing this challenge!";
    }

    /**
     * Extract exercise focus from metadata
     */
    @SuppressWarnings("unchecked")
    private List<String> extractExerciseFocus(Challenge challenge) {
        if (challenge.getPersonalizationData() == null) {
            return Collections.emptyList();
        }

        Object exerciseFocus = challenge.getPersonalizationData().get("exerciseFocus");
        if (exerciseFocus instanceof List) {
            return (List<String>) exerciseFocus;
        }

        return Collections.emptyList();
    }

    /**
     * Extract safety note from metadata
     */
    private String extractSafetyNote(Challenge challenge) {
        if (challenge.getPersonalizationData() == null) {
            return null;
        }

        Object safetyNote = challenge.getPersonalizationData().get("safetyNote");
        return safetyNote != null ? safetyNote.toString() : null;
    }

    /**
     * Generate contextual tips based on challenge type AND journey
     */
    private List<String> generateTips(Challenge challenge) {
        List<String> tips = new ArrayList<>();

        // Type-based tips
        tips.add(switch (challenge.getType()) {
            case DAILY -> "Complete this today for maximum rewards!";
            case WEEKLY -> "Pace yourself - you have all week!";
            case MONTHLY -> "Break it down into weekly goals";
            default -> "Stay consistent!";
        });

        // Difficulty-based tips
        if (challenge.getDifficultyLevel() != null) {
            String difficultyTip = switch (challenge.getDifficultyLevel()) {
                case BEGINNER -> "Focus on form and consistency";
                case INTERMEDIATE -> "Challenge yourself, but listen to your body";
                case ADVANCED -> "Push your limits safely";
                case ELITE -> "Elite challenge - give it your best!";
            };
            tips.add(difficultyTip);
        }

        // Journey phase tips
        if (challenge.getJourneyPhase() != null) {
            String phaseTip = switch (challenge.getJourneyPhase()) {
                case "foundation" -> "Building strong fundamentals";
                case "building" -> "Progressive overload - increase gradually";
                case "mastery" -> "Fine-tuning technique and performance";
                default -> null;
            };
            if (phaseTip != null) {
                tips.add(phaseTip);
            }
        }

        // Journey tag-specific tips
        if (challenge.getUserJourneyTags() != null) {
            if (challenge.getUserJourneyTags().contains("bodyweight")) {
                tips.add("No equipment needed - perfect for home workouts");
            }
            if (challenge.getUserJourneyTags().contains("milestone")) {
                tips.add("Milestone challenge - marks a significant achievement!");
            }
            if (challenge.getUserJourneyTags().contains("fundamentals")) {
                tips.add("Master the basics - they're the foundation of all progress");
            }
            if (challenge.getUserJourneyTags().contains("progression")) {
                tips.add("Track your progress - small improvements compound over time");
            }
        }

        // Objective-specific tips
        if (challenge.getObjectiveType() != null) {
            String objectiveTip = switch (challenge.getObjectiveType()) {
                case REPS -> "Break it into manageable sets";
                case DURATION -> "Set a timer and stay focused";
                case EXERCISES -> "Mix up your routine for variety";
                case VOLUME_BASED -> "Track your total weight lifted";
                case PERSONAL_RECORDS -> "Warm up thoroughly before attempting PRs";
                default -> null;
            };
            if (objectiveTip != null) {
                tips.add(objectiveTip);
            }
        }

        return tips;
    }

    /**
     * FULLY IMPLEMENTED: Extract prerequisite challenge names
     * Looks up actual challenge names from template IDs
     */
    private List<String> extractPrerequisiteNames(Challenge challenge) {
        if (challenge.getPrerequisiteChallengeIds() == null ||
                challenge.getPrerequisiteChallengeIds().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return challenge.getPrerequisiteChallengeIds().stream()
                    .map(templateId -> {
                        try {
                            // Look up template by ID
                            Optional<ChallengeTemplate> template = templateRepository.findById(templateId);
                            return template.map(ChallengeTemplate::getName).orElse(null);
                        } catch (Exception e) {
                            log.warn("Failed to find prerequisite template: {}", templateId);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error extracting prerequisite names: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * FULLY IMPLEMENTED: Extract unlock challenge names
     * Looks up actual challenge names from template IDs
     */
    private List<String> extractUnlockNames(Challenge challenge) {
        if (challenge.getUnlocksChallengeIds() == null ||
                challenge.getUnlocksChallengeIds().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return challenge.getUnlocksChallengeIds().stream()
                    .map(templateId -> {
                        try {
                            // Look up template by ID
                            Optional<ChallengeTemplate> template = templateRepository.findById(templateId);
                            return template.map(ChallengeTemplate::getName).orElse(null);
                        } catch (Exception e) {
                            log.warn("Failed to find unlock template: {}", templateId);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error extracting unlock names: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Batch lookup for better performance
     * Use this when mapping multiple challenges at once
     */
    public List<ChallengeDto> toDtoList(List<Challenge> challenges, Map<String, UserChallenge> userChallengeMap) {
        if (challenges == null || challenges.isEmpty()) {
            return Collections.emptyList();
        }

        // Pre-load all template names for better performance
        Set<String> allTemplateIds = challenges.stream()
                .flatMap(c -> {
                    List<String> ids = new ArrayList<>();
                    if (c.getPrerequisiteChallengeIds() != null) {
                        ids.addAll(c.getPrerequisiteChallengeIds());
                    }
                    if (c.getUnlocksChallengeIds() != null) {
                        ids.addAll(c.getUnlocksChallengeIds());
                    }
                    return ids.stream();
                })
                .collect(Collectors.toSet());

        // Batch lookup templates
        List<ChallengeTemplate> templates = templateRepository.findAllById(allTemplateIds);
        Map<String, String> templateNameMap = templates.stream()
                .collect(Collectors.toMap(ChallengeTemplate::getId, ChallengeTemplate::getName));

        // Cache template names for this batch
        this.templateNameCache = templateNameMap;

        try {
            return challenges.stream()
                    .map(challenge -> {
                        UserChallenge userChallenge = userChallengeMap.get(challenge.getId());
                        return toDto(challenge, userChallenge);
                    })
                    .collect(Collectors.toList());
        } finally {
            // Clear cache after batch operation
            this.templateNameCache = null;
        }
    }

    // Thread-local cache for batch operations
    private Map<String, String> templateNameCache = null;

    /**
     * Optimized prerequisite lookup using cache
     */
    private List<String> extractPrerequisiteNamesCached(Challenge challenge) {
        if (templateNameCache != null) {
            // Use cached names
            return challenge.getPrerequisiteChallengeIds().stream()
                    .map(templateNameCache::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return extractPrerequisiteNames(challenge);
    }

    /**
     * Optimized unlock lookup using cache
     */
    private List<String> extractUnlockNamesCached(Challenge challenge) {
        if (templateNameCache != null) {
            // Use cached names
            return challenge.getUnlocksChallengeIds().stream()
                    .map(templateNameCache::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return extractUnlockNames(challenge);
    }
}