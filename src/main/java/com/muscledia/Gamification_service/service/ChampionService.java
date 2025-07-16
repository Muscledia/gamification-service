package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Champion;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.enums.ChampionCriteriaType;
import com.muscledia.Gamification_service.repository.ChampionRepository;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChampionService {

    private final ChampionRepository championRepository;
    private final UserGamificationProfileRepository userProfileRepository;

    /**
     * Create a new champion
     */
    @Transactional
    public Champion createChampion(Champion champion) {
        log.info("Creating new champion: {}", champion.getName());

        // Validate champion doesn't already exist
        if (championRepository.existsByName(champion.getName())) {
            throw new IllegalArgumentException("Champion with name '" + champion.getName() + "' already exists");
        }

        // Set creation timestamp
        champion.setCreatedAt(Instant.now());
        champion.setUpdatedAt(Instant.now());

        Champion savedChampion = championRepository.save(champion);
        log.info("Champion created successfully: {}", savedChampion.getId());
        return savedChampion;
    }

    /**
     * Get all champions with optional filtering
     */
    public List<Champion> getAllChampions(ChampionCriteriaType criteriaType, Integer maxDifficulty, String exerciseId,
            String muscleGroupId) {
        if (criteriaType != null && maxDifficulty != null) {
            return championRepository.findByCriteriaTypeAndBaseDifficultyLessThanEqual(criteriaType, maxDifficulty);
        } else if (criteriaType != null) {
            return championRepository.findByCriteriaType(criteriaType);
        } else if (exerciseId != null) {
            return championRepository.findByRequiredExerciseId(exerciseId);
        } else if (muscleGroupId != null) {
            return championRepository.findByMuscleGroupId(muscleGroupId);
        } else if (maxDifficulty != null) {
            return championRepository.findByBaseDifficultyLessThanEqual(maxDifficulty);
        } else {
            return championRepository.findAll();
        }
    }

    /**
     * Get champions by difficulty level
     */
    public List<Champion> getChampionsByDifficulty(int difficulty) {
        return championRepository.findByBaseDifficulty(difficulty);
    }

    /**
     * Get champions by difficulty range
     */
    public List<Champion> getChampionsByDifficultyRange(int minDifficulty, int maxDifficulty) {
        return championRepository.findByBaseDifficultyBetween(minDifficulty, maxDifficulty);
    }

    /**
     * Get champions for specific exercise
     */
    public List<Champion> getChampionsForExercise(String exerciseId, Integer maxDifficulty) {
        if (maxDifficulty != null) {
            return championRepository.findByRequiredExerciseIdAndBaseDifficultyLessThanEqual(exerciseId, maxDifficulty);
        } else {
            return championRepository.findByRequiredExerciseId(exerciseId);
        }
    }

    /**
     * Get champions for specific muscle group
     */
    public List<Champion> getChampionsForMuscleGroup(String muscleGroupId, Integer maxDifficulty) {
        if (maxDifficulty != null) {
            return championRepository.findByMuscleGroupIdAndBaseDifficultyLessThanEqual(muscleGroupId, maxDifficulty);
        } else {
            return championRepository.findByMuscleGroupId(muscleGroupId);
        }
    }

    /**
     * Get general champions (not exercise-specific)
     */
    public List<Champion> getGeneralChampions() {
        return championRepository.findGeneralChampions();
    }

    /**
     * Get exercise-specific champions
     */
    public List<Champion> getExerciseSpecificChampions() {
        return championRepository.findExerciseSpecificChampions();
    }

    /**
     * Check if user meets champion criteria
     */
    public boolean checkChampionCriteria(Long userId, String championId, Map<String, Object> userStats) {
        log.info("Checking champion criteria for user {} and champion {}", userId, championId);

        Champion champion = championRepository.findById(championId)
                .orElseThrow(() -> new IllegalArgumentException("Champion not found: " + championId));

        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        // Check if user level is sufficient for this champion difficulty
        if (userProfile.getLevel() < champion.getBaseDifficulty()) {
            log.info("User {} level {} insufficient for champion difficulty {}", userId, userProfile.getLevel(),
                    champion.getBaseDifficulty());
            return false;
        }

        return evaluateChampionCriteria(champion, userStats);
    }

    /**
     * Get eligible champions for a user
     */
    public List<Champion> getEligibleChampions(Long userId, Map<String, Object> userStats) {
        log.info("Finding eligible champions for user {}", userId);

        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        // Get all champions suitable for user level
        List<Champion> allChampions = championRepository.findByBaseDifficultyLessThanEqual(userProfile.getLevel());

        // Filter for eligible champions
        return allChampions.stream()
                .filter(champion -> evaluateChampionCriteria(champion, userStats))
                .collect(Collectors.toList());
    }

    /**
     * Award champion status to user
     */
    @Transactional
    public UserGamificationProfile awardChampion(Long userId, String championId) {
        log.info("Awarding champion {} to user {}", championId, userId);

        // Get user profile
        UserGamificationProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        // Get champion
        Champion champion = championRepository.findById(championId)
                .orElseThrow(() -> new IllegalArgumentException("Champion not found: " + championId));

        // Check if user already has this champion
        boolean alreadyHasChampion = userProfile.getEarnedBadges().stream()
                .anyMatch(ub -> ub.getBadgeId().equals(championId)); // Note: This should be improved with proper
                                                                     // champion tracking

        if (alreadyHasChampion) {
            log.warn("User {} already has champion {}", userId, championId);
            return userProfile;
        }

        // Award significant points for champion achievement
        int championPoints = calculateChampionPoints(champion);
        userProfile.setPoints(userProfile.getPoints() + championPoints);

        // Check for level up
        int newLevel = calculateLevel(userProfile.getPoints());
        if (newLevel > userProfile.getLevel()) {
            userProfile.setLevel(newLevel);
            userProfile.setLastLevelUpDate(Instant.now());
            log.info("User {} leveled up to level {} from champion award", userId, newLevel);
        }

        // TODO: Add proper champion tracking to user profile
        // For now, we'll create a special badge entry
        // This should be improved with a dedicated champion tracking system

        UserGamificationProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("Champion {} awarded successfully to user {}", championId, userId);
        return savedProfile;
    }

    /**
     * Get champion statistics
     */
    public Map<String, Object> getChampionStatistics() {
        log.info("Getting champion statistics");

        Map<String, Object> stats = new HashMap<>();

        long totalChampions = championRepository.count();
        stats.put("totalChampions", totalChampions);

        // Count by difficulty level
        Map<Integer, Long> difficultyDistribution = new HashMap<>();
        for (int difficulty = 1; difficulty <= 5; difficulty++) {
            difficultyDistribution.put(difficulty, (long) championRepository.findByBaseDifficulty(difficulty).size());
        }
        stats.put("difficultyDistribution", difficultyDistribution);

        // Count by criteria type
        Map<ChampionCriteriaType, Long> criteriaTypeCount = new HashMap<>();
        for (ChampionCriteriaType type : ChampionCriteriaType.values()) {
            criteriaTypeCount.put(type, (long) championRepository.findByCriteriaType(type).size());
        }
        stats.put("championsByCriteriaType", criteriaTypeCount);

        // General vs exercise-specific champions
        long generalChampions = championRepository.findGeneralChampions().size();
        long exerciseSpecificChampions = championRepository.findExerciseSpecificChampions().size();
        stats.put("generalChampions", generalChampions);
        stats.put("exerciseSpecificChampions", exerciseSpecificChampions);

        return stats;
    }

    /**
     * Get champions ordered by difficulty
     */
    public List<Champion> getChampionsByDifficultyAscending() {
        return championRepository.findAllByOrderByBaseDifficultyAsc();
    }

    /**
     * Get champions ordered by difficulty (hardest first)
     */
    public List<Champion> getChampionsByDifficultyDescending() {
        return championRepository.findAllByOrderByBaseDifficultyDesc();
    }

    /**
     * Get recently created champions
     */
    public List<Champion> getRecentlyCreatedChampions() {
        return championRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Update champion
     */
    @Transactional
    public Champion updateChampion(String championId, Champion championUpdates) {
        log.info("Updating champion: {}", championId);

        Champion existingChampion = championRepository.findById(championId)
                .orElseThrow(() -> new IllegalArgumentException("Champion not found: " + championId));

        // Update fields
        if (championUpdates.getName() != null) {
            existingChampion.setName(championUpdates.getName());
        }
        if (championUpdates.getDescription() != null) {
            existingChampion.setDescription(championUpdates.getDescription());
        }
        if (championUpdates.getImageUrl() != null) {
            existingChampion.setImageUrl(championUpdates.getImageUrl());
        }
        if (championUpdates.getBaseDifficulty() != 0) {
            existingChampion.setBaseDifficulty(championUpdates.getBaseDifficulty());
        }
        if (championUpdates.getCriteriaType() != null) {
            existingChampion.setCriteriaType(championUpdates.getCriteriaType());
        }
        if (championUpdates.getCriteriaParams() != null) {
            existingChampion.setCriteriaParams(championUpdates.getCriteriaParams());
        }

        existingChampion.setUpdatedAt(Instant.now());

        Champion savedChampion = championRepository.save(existingChampion);
        log.info("Champion updated successfully: {}", championId);
        return savedChampion;
    }

    /**
     * Delete champion
     */
    @Transactional
    public void deleteChampion(String championId) {
        log.info("Deleting champion: {}", championId);

        if (!championRepository.existsById(championId)) {
            throw new IllegalArgumentException("Champion not found: " + championId);
        }

        // TODO: Consider removing from user profiles or marking as deprecated
        championRepository.deleteById(championId);
        log.info("Champion deleted successfully: {}", championId);
    }

    /**
     * Private helper method to evaluate champion criteria
     */
    private boolean evaluateChampionCriteria(Champion champion, Map<String, Object> userStats) {
        ChampionCriteriaType criteriaType = champion.getCriteriaType();
        Map<String, Object> criteriaParams = champion.getCriteriaParams();

        if (criteriaType == null || criteriaParams == null) {
            return false;
        }

        switch (criteriaType) {
            case PERSONAL_RECORD_WEIGHT:
                return checkPersonalRecordWeight(userStats, criteriaParams);
            case PERSONAL_RECORD_REPS:
                return checkPersonalRecordReps(userStats, criteriaParams);
            case STRENGTH_MILESTONE:
                return checkStrengthMilestone(userStats, criteriaParams);
            case ENDURANCE_MILESTONE:
                return checkEnduranceMilestone(userStats, criteriaParams);
            case CONSISTENCY_CHAMPION:
                return checkConsistencyChampion(userStats, criteriaParams);
            case TRANSFORMATION_MASTER:
                return checkTransformationMaster(userStats, criteriaParams);
            case STRENGTH_PROGRESSION:
                return checkStrengthProgression(userStats, criteriaParams);
            case COMPETITION_WINNER:
                return checkCompetitionWinner(userStats, criteriaParams);
            case LEADERBOARD_DOMINATION:
                return checkLeaderboardDomination(userStats, criteriaParams);
            case RECORD_BREAKER:
                return checkRecordBreaker(userStats, criteriaParams);
            case COMMUNITY_LEADER:
                return checkCommunityLeader(userStats, criteriaParams);
            case TRAINING_VOLUME_ELITE:
                return checkTrainingVolumeElite(userStats, criteriaParams);
            case INTENSITY_MASTER:
                return checkIntensityMaster(userStats, criteriaParams);
            case POWER_TO_WEIGHT_RATIO:
                return checkPowerToWeightRatio(userStats, criteriaParams);
            case VETERAN_ACHIEVER:
                return checkVeteranAchiever(userStats, criteriaParams);
            case SUSTAINED_EXCELLENCE:
                return checkSustainedExcellence(userStats, criteriaParams);
            // Add more criteria evaluations as needed
            default:
                log.warn("Unsupported champion criteria type: {}", criteriaType);
                return false;
        }
    }

    /**
     * Private helper methods for specific champion criteria evaluations
     */
    private boolean checkPersonalRecordWeight(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        String exerciseId = (String) criteriaParams.get("exerciseId");
        Double requiredWeight = (Double) criteriaParams.get("targetWeightKg");

        @SuppressWarnings("unchecked")
        Map<String, Double> userPRs = (Map<String, Double>) userStats.get("personalRecords");

        if (userPRs == null || exerciseId == null || requiredWeight == null) {
            return false;
        }

        Double userPR = userPRs.get(exerciseId);
        return userPR != null && userPR >= requiredWeight;
    }

    private boolean checkPersonalRecordReps(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        String exerciseId = (String) criteriaParams.get("exerciseId");
        Integer requiredReps = (Integer) criteriaParams.get("targetReps");

        @SuppressWarnings("unchecked")
        Map<String, Integer> userRepRecords = (Map<String, Integer>) userStats.get("repRecords");

        if (userRepRecords == null || exerciseId == null || requiredReps == null) {
            return false;
        }

        Integer userRecord = userRepRecords.get(exerciseId);
        return userRecord != null && userRecord >= requiredReps;
    }

    private boolean checkStrengthMilestone(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double userBodyWeight = (Double) userStats.get("bodyWeight");
        String exerciseId = (String) criteriaParams.get("exerciseId");
        Double multiplier = (Double) criteriaParams.get("bodyWeightMultiplier");

        if (userBodyWeight == null || exerciseId == null || multiplier == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Double> userPRs = (Map<String, Double>) userStats.get("personalRecords");

        if (userPRs == null) {
            return false;
        }

        Double userPR = userPRs.get(exerciseId);
        return userPR != null && userPR >= (userBodyWeight * multiplier);
    }

    private boolean checkEnduranceMilestone(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double userTime = (Double) userStats.get("bestTime");
        Double requiredTime = (Double) criteriaParams.get("targetTimeMinutes");

        return userTime != null && requiredTime != null && userTime <= requiredTime;
    }

    private boolean checkConsistencyChampion(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer userStreak = (Integer) userStats.get("longestWorkoutStreak");
        Integer requiredStreak = (Integer) criteriaParams.get("requiredStreakDays");

        return userStreak != null && requiredStreak != null && userStreak >= requiredStreak;
    }

    private boolean checkTransformationMaster(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double strengthImprovement = (Double) userStats.get("strengthImprovementPercentage");
        Double requiredImprovement = (Double) criteriaParams.get("minimumImprovementPercentage");

        return strengthImprovement != null && requiredImprovement != null && strengthImprovement >= requiredImprovement;
    }

    private boolean checkStrengthProgression(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double progressionRate = (Double) userStats.get("strengthProgressionRate");
        Double requiredRate = (Double) criteriaParams.get("minimumProgressionRate");

        return progressionRate != null && requiredRate != null && progressionRate >= requiredRate;
    }

    private boolean checkCompetitionWinner(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer competitionWins = (Integer) userStats.get("competitionWins");
        Integer requiredWins = (Integer) criteriaParams.get("minimumWins");

        return competitionWins != null && requiredWins != null && competitionWins >= requiredWins;
    }

    private boolean checkLeaderboardDomination(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer daysAtTop = (Integer) userStats.get("daysAtTopOfLeaderboard");
        Integer requiredDays = (Integer) criteriaParams.get("requiredDaysAtTop");

        return daysAtTop != null && requiredDays != null && daysAtTop >= requiredDays;
    }

    private boolean checkRecordBreaker(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer recordsBroken = (Integer) userStats.get("platformRecordsBroken");
        Integer requiredRecords = (Integer) criteriaParams.get("minimumRecordsBroken");

        return recordsBroken != null && requiredRecords != null && recordsBroken >= requiredRecords;
    }

    private boolean checkCommunityLeader(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer helpedUsers = (Integer) userStats.get("usersHelped");
        Integer requiredHelped = (Integer) criteriaParams.get("minimumUsersHelped");

        return helpedUsers != null && requiredHelped != null && helpedUsers >= requiredHelped;
    }

    private boolean checkTrainingVolumeElite(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double trainingVolume = (Double) userStats.get("totalTrainingVolume");
        Double requiredVolume = (Double) criteriaParams.get("minimumTrainingVolume");

        return trainingVolume != null && requiredVolume != null && trainingVolume >= requiredVolume;
    }

    private boolean checkIntensityMaster(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double averageIntensity = (Double) userStats.get("averageWorkoutIntensity");
        Double requiredIntensity = (Double) criteriaParams.get("minimumAverageIntensity");

        return averageIntensity != null && requiredIntensity != null && averageIntensity >= requiredIntensity;
    }

    private boolean checkPowerToWeightRatio(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double powerToWeight = (Double) userStats.get("powerToWeightRatio");
        Double requiredRatio = (Double) criteriaParams.get("minimumPowerToWeightRatio");

        return powerToWeight != null && requiredRatio != null && powerToWeight >= requiredRatio;
    }

    private boolean checkVeteranAchiever(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Integer daysActive = (Integer) userStats.get("daysActivOnPlatform");
        Integer requiredDays = (Integer) criteriaParams.get("minimumActiveDays");

        return daysActive != null && requiredDays != null && daysActive >= requiredDays;
    }

    private boolean checkSustainedExcellence(Map<String, Object> userStats, Map<String, Object> criteriaParams) {
        Double consistencyScore = (Double) userStats.get("consistencyScore");
        Double requiredScore = (Double) criteriaParams.get("minimumConsistencyScore");

        return consistencyScore != null && requiredScore != null && consistencyScore >= requiredScore;
    }

    /**
     * Calculate champion points based on difficulty
     */
    private int calculateChampionPoints(Champion champion) {
        int baseDifficulty = champion.getBaseDifficulty();

        // Champions award significantly more points than regular badges
        switch (baseDifficulty) {
            case 1:
                return 500;
            case 2:
                return 750;
            case 3:
                return 1000;
            case 4:
                return 1500;
            case 5:
                return 2000;
            default:
                return 1000;
        }
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