package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.UserPerformanceMetrics;
import com.muscledia.Gamification_service.model.enums.ChallengeStatus;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPerformanceAnalyzer {
    private final UserChallengeRepository userChallengeRepository;
    private final UserGamificationService gamificationService;
    private final ChallengeRepository challengeRepository; // ADD THIS

    public UserPerformanceMetrics analyzeUser(Long userId) {
        // Get recent challenge history (last 30 days)
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        List<UserChallenge> recentChallenges = userChallengeRepository
                .findByUserIdAndStartedAtAfter(userId, thirtyDaysAgo);

        if (recentChallenges.isEmpty()) {
            return createDefaultMetrics(userId);
        }

        double completionRate = calculateCompletionRate(recentChallenges);
        Map<ObjectiveType, Double> objectivePerformance = analyzeObjectivePerformance(recentChallenges);
        int averageCompletionTime = calculateAverageCompletionTime(recentChallenges);

        UserGamificationProfile profile = gamificationService.getUserProfile(userId);

        return UserPerformanceMetrics.builder()
                .userId(userId)
                .recentCompletionRate(completionRate)
                .currentLevel(profile.getLevel())
                .totalPoints(profile.getPoints())
                .objectivePerformance(objectivePerformance)
                .averageCompletionTimeHours(averageCompletionTime)
                .consecutiveCompletions(calculateConsecutiveCompletions(recentChallenges))
                .preferredChallengeTypes(analyzePreferredTypes(recentChallenges))
                .totalChallengesAttempted(recentChallenges.size())
                .totalChallengesCompleted((int) recentChallenges.stream()
                        .filter(c -> c.getStatus() == ChallengeStatus.COMPLETED)
                        .count())
                .consistencyScore(calculateConsistencyScore(recentChallenges))
                .build();
    }

    // ADD MISSING METHODS
    private UserPerformanceMetrics createDefaultMetrics(Long userId) {
        UserGamificationProfile profile = gamificationService.getUserProfile(userId);

        return UserPerformanceMetrics.builder()
                .userId(userId)
                .recentCompletionRate(0.5) // Neutral starting point
                .currentLevel(profile.getLevel())
                .totalPoints(profile.getPoints())
                .objectivePerformance(Map.of())
                .averageCompletionTimeHours(48) // 2 days default
                .consecutiveCompletions(0)
                .preferredChallengeTypes(Set.of())
                .totalChallengesAttempted(0)
                .totalChallengesCompleted(0)
                .consistencyScore(0.0)
                .build();
    }

    private double calculateCompletionRate(List<UserChallenge> challenges) {
        if (challenges.isEmpty()) return 0.0;

        long completed = challenges.stream()
                .filter(c -> c.getStatus() == ChallengeStatus.COMPLETED)
                .count();
        return (double) completed / challenges.size();
    }

    private Map<ObjectiveType, Double> analyzeObjectivePerformance(List<UserChallenge> challenges) {
        return challenges.stream()
                .filter(c -> c.getStatus() == ChallengeStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        this::getChallengeObjectiveType,
                        Collectors.averagingDouble(c ->
                                (double) c.getCurrentProgress() / c.getTargetValue())));
    }

    private ObjectiveType getChallengeObjectiveType(UserChallenge userChallenge) {
        // Get the challenge and return its objective type
        return challengeRepository.findById(userChallenge.getChallengeId())
                .map(Challenge::getObjectiveType)
                .orElse(ObjectiveType.EXERCISES); // Default fallback
    }

    private int calculateAverageCompletionTime(List<UserChallenge> challenges) {
        return challenges.stream()
                .filter(c -> c.getStatus() == ChallengeStatus.COMPLETED)
                .filter(c -> c.getCompletedAt() != null && c.getStartedAt() != null)
                .mapToInt(c -> (int) Duration.between(c.getStartedAt(), c.getCompletedAt()).toHours())
                .reduce(0, Integer::sum) / Math.max(1, challenges.size());
    }

    private int calculateConsecutiveCompletions(List<UserChallenge> challenges) {
        int consecutive = 0;
        int current = 0;

        for (UserChallenge challenge : challenges) {
            if (challenge.getStatus() == ChallengeStatus.COMPLETED) {
                current++;
                consecutive = Math.max(consecutive, current);
            } else {
                current = 0;
            }
        }

        return consecutive;
    }

    private Set<ChallengeType> analyzePreferredTypes(List<UserChallenge> challenges) {
        Map<ChallengeType, Long> typeFrequency = challenges.stream()
                .filter(c -> c.getStatus() == ChallengeStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        this::getChallengeType,
                        Collectors.counting()));

        return typeFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 1) // At least 2 completions
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private ChallengeType getChallengeType(UserChallenge userChallenge) {
        return challengeRepository.findById(userChallenge.getChallengeId())
                .map(Challenge::getType)
                .orElse(ChallengeType.DAILY); // Default fallback
    }

    private double calculateConsistencyScore(List<UserChallenge> challenges) {
        if (challenges.isEmpty()) return 0.0;

        // Simple consistency score based on completion rate and consecutive completions
        double completionRate = calculateCompletionRate(challenges);
        int consecutive = calculateConsecutiveCompletions(challenges);

        return (completionRate * 0.7) + (Math.min(consecutive / 10.0, 1.0) * 0.3);
    }
}
