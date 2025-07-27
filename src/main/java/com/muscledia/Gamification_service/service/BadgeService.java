package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.UserBadge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import com.muscledia.Gamification_service.repository.BadgeRepository;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserGamificationProfileRepository userProfileRepository;

    /**
     * Create a new badge
     */
    @Transactional
    public Badge createBadge(Badge badge) {
        log.info("Creating new badge: {}", badge.getName());

        // Validate badge doesn't already exist
        if (badgeRepository.existsByName(badge.getName())) {
            throw new IllegalArgumentException("Badge with name '" + badge.getName() + "' already exists");
        }

        // Set creation timestamp
        badge.setCreatedAt(Instant.now());

        Badge savedBadge = badgeRepository.save(badge);
        log.info("Badge created successfully: {}", savedBadge.getBadgeId());
        return savedBadge;
    }

    /**
     * Get all badges with optional filtering
     */
    public List<Badge> getAllBadges(BadgeType badgeType, BadgeCriteriaType criteriaType) {
        if (badgeType != null && criteriaType != null) {
            return badgeRepository.findByBadgeTypeAndCriteriaType(badgeType, criteriaType);
        } else if (badgeType != null) {
            return badgeRepository.findByBadgeType(badgeType);
        } else if (criteriaType != null) {
            return badgeRepository.findByCriteriaType(criteriaType);
        } else {
            return badgeRepository.findAll();
        }
    }

    /**
     * Get badges by minimum points threshold
     */
    public List<Badge> getBadgesByMinPoints(int minPoints) {
        return badgeRepository.findByPointsAwardedGreaterThanEqual(minPoints);
    }

    /**
     * Award a badge to a user
     */
    @Transactional
    public UserGamificationProfile awardBadge(Long userId, String badgeId) {
        log.info("Awarding badge {} to user {}", badgeId, userId);

        // Get user profile
        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        // Get badge
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalArgumentException("Badge not found: " + badgeId));

        // Check if user already has this badge
        boolean alreadyHasBadge = userProfile.getEarnedBadges().stream()
                .anyMatch(ub -> ub.getBadgeId().equals(badgeId));

        if (alreadyHasBadge) {
            log.warn("User {} already has badge {}", userId, badgeId);
            return userProfile;
        }

        // Create user badge
        UserBadge userBadge = new UserBadge();
        userBadge.setBadgeId(badgeId);
        userBadge.setEarnedAt(Instant.now());

        // Add badge to user profile
        userProfile.getEarnedBadges().add(userBadge);

        // Award points
        userProfile.setPoints(userProfile.getPoints() + badge.getPointsAwarded());

        // Check for level up
        int newLevel = calculateLevel(userProfile.getPoints());
        if (newLevel > userProfile.getLevel()) {
            userProfile.setLevel(newLevel);
            userProfile.setLastLevelUpDate(Instant.now());
            log.info("User {} leveled up to level {}", userId, newLevel);
        }

        UserGamificationProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("Badge {} awarded successfully to user {}", badgeId, userId);
        return savedProfile;
    }

    /**
     * Check if user meets badge criteria
     */
    public boolean checkBadgeCriteria(Long userId, String badgeId, Map<String, Object> userStats) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalArgumentException("Badge not found: " + badgeId));

        return evaluateCriteria(badge, userStats);
    }

    /**
     * Get eligible badges for a user based on their stats
     */
    public List<Badge> getEligibleBadges(Long userId, Map<String, Object> userStats) {
        log.info("Finding eligible badges for user {}", userId);

        // Get user profile to check already earned badges
        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        Set<String> earnedBadgeIds = userProfile.getEarnedBadges().stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        // Get all badges and filter for eligible ones
        List<Badge> allBadges = badgeRepository.findAll();

        return allBadges.stream()
                .filter(badge -> !earnedBadgeIds.contains(badge.getBadgeId())) // Not already earned
                .filter(badge -> evaluateCriteria(badge, userStats)) // Meets criteria
                .collect(Collectors.toList());
    }

    /**
     * Get user's earned badges
     */
    public List<Badge> getUserBadges(Long userId) {
        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        List<String> badgeIds = userProfile.getEarnedBadges().stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toList());

        return badgeRepository.findAllById(badgeIds);
    }

    /**
     * Get badge statistics
     */
    public Map<String, Object> getBadgeStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalBadges = badgeRepository.count();
        stats.put("totalBadges", totalBadges);

        // Count by badge type
        Map<BadgeType, Long> badgeTypeCount = new HashMap<>();
        for (BadgeType type : BadgeType.values()) {
            badgeTypeCount.put(type, (long) badgeRepository.findByBadgeType(type).size());
        }
        stats.put("badgesByType", badgeTypeCount);

        // Count by criteria type
        Map<BadgeCriteriaType, Long> criteriaTypeCount = new HashMap<>();
        for (BadgeCriteriaType type : BadgeCriteriaType.values()) {
            criteriaTypeCount.put(type, (long) badgeRepository.findByCriteriaType(type).size());
        }
        stats.put("badgesByCriteriaType", criteriaTypeCount);

        return stats;
    }

    /**
     * Delete a badge
     */
    @Transactional
    public void deleteBadge(String badgeId) {
        log.info("Deleting badge: {}", badgeId);

        if (!badgeRepository.existsById(badgeId)) {
            throw new IllegalArgumentException("Badge not found: " + badgeId);
        }

        // TODO: Consider removing from user profiles or marking as deprecated
        badgeRepository.deleteById(badgeId);
        log.info("Badge deleted successfully: {}", badgeId);
    }

    /**
     * Private helper method to evaluate badge criteria
     */
    private boolean evaluateCriteria(Badge badge, Map<String, Object> userStats) {
        BadgeCriteriaType criteriaType = badge.getCriteriaType();
        Map<String, Object> criteriaParams = badge.getCriteriaParams();

        if (criteriaType == null || criteriaParams == null) {
            return false;
        }

        switch (criteriaType) {
            case WORKOUT_COUNT:
                return checkWorkoutCount(userStats, criteriaParams);
            case WORKOUT_STREAK:
                return checkWorkoutStreak(userStats, criteriaParams);
            case PERSONAL_RECORD:
                return checkPersonalRecord(userStats, criteriaParams);
            case POINTS_EARNED:
                return checkPointsEarned(userStats, criteriaParams);
            case LEVEL_REACHED:
                return checkLevelReached(userStats, criteriaParams);
            case WEIGHT_LIFTED_TOTAL:
                return checkWeightLifted(userStats, criteriaParams);
            case EXERCISE_COUNT:
                return checkExerciseCount(userStats, criteriaParams);
            case LOGIN_STREAK:
                return checkLoginStreak(userStats, criteriaParams);
            // Add more criteria evaluations as needed
            default:
                log.warn("Unsupported criteria type: {}", criteriaType);
                return false;
        }
    }

    /**
     * Private helper methods for specific criteria evaluations
     */
    private boolean checkWorkoutCount(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer userWorkoutCount = (Integer) userStats.get("workoutCount");
        Integer requiredCount = (Integer) criteriaParams.get("targetValue");
        return userWorkoutCount != null && requiredCount != null && userWorkoutCount >= requiredCount;
    }

    private boolean checkWorkoutStreak(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer userStreak = (Integer) userStats.get("currentWorkoutStreak");
        Integer requiredStreak = (Integer) criteriaParams.get("targetValue");
        return userStreak != null && requiredStreak != null && userStreak >= requiredStreak;
    }

    private boolean checkPersonalRecord(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        // Implementation depends on how PR data is structured
        String exerciseId = (String) criteriaParams.get("exerciseId");
        Double requiredWeight = (Double) criteriaParams.get("targetWeight");

        @SuppressWarnings("unchecked")
        Map<String, Double> userPRs = (Map<String, Double>) userStats.get("personalRecords");

        if (userPRs == null || exerciseId == null || requiredWeight == null) {
            return false;
        }

        Double userPR = userPRs.get(exerciseId);
        return userPR != null && userPR >= requiredWeight;
    }

    private boolean checkPointsEarned(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer userPoints = (Integer) userStats.get("totalPoints");
        Integer requiredPoints = (Integer) criteriaParams.get("targetValue");
        return userPoints != null && requiredPoints != null && userPoints >= requiredPoints;
    }

    private boolean checkLevelReached(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer userLevel = (Integer) userStats.get("currentLevel");
        Integer requiredLevel = (Integer) criteriaParams.get("targetValue");
        return userLevel != null && requiredLevel != null && userLevel >= requiredLevel;
    }

    private boolean checkWeightLifted(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double userWeightLifted = (Double) userStats.get("totalWeightLifted");
        Double requiredWeight = (Double) criteriaParams.get("targetValue");
        return userWeightLifted != null && requiredWeight != null && userWeightLifted >= requiredWeight;
    }

    private boolean checkExerciseCount(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer userExerciseCount = (Integer) userStats.get("uniqueExerciseCount");
        Integer requiredCount = (Integer) criteriaParams.get("targetValue");
        return userExerciseCount != null && requiredCount != null && userExerciseCount >= requiredCount;
    }

    private boolean checkLoginStreak(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer userLoginStreak = (Integer) userStats.get("currentLoginStreak");
        Integer requiredStreak = (Integer) criteriaParams.get("targetValue");
        return userLoginStreak != null && requiredStreak != null && userLoginStreak >= requiredStreak;
    }

    /**
     * Calculate user level based on points
     */
    private int calculateLevel(int points) {
        // Simple level calculation - can be made more sophisticated
        if (points < 100)
            return 1;
        if (points < 300)
            return 2;
        if (points < 600)
            return 3;
        if (points < 1000)
            return 4;
        if (points < 1500)
            return 5;
        if (points < 2100)
            return 6;
        if (points < 2800)
            return 7;
        if (points < 3600)
            return 8;
        if (points < 4500)
            return 9;
        return 10 + (points - 4500) / 1000; // Level 10+ requires 1000 points each
    }
}