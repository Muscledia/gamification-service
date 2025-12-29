package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CLEAR COIN ECONOMY: Users know exactly how to earn
 *
 * Uses blocking repository (MongoRepository) - matches existing architecture
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FitnessCoinsService {
    private final UserGamificationProfileRepository profileRepository;

    /**
     * Award coins for workout completion
     */
    @Transactional
    public CoinReward awardWorkoutCoins(
            Long userId,
            int durationMinutes,
            int personalRecordsAchieved,
            int currentStreak) {

        int coins = calculateWorkoutCoins(durationMinutes, personalRecordsAchieved, currentStreak);

        UserGamificationProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        // Update coins
        int currentCoins = profile.getFitnessCoins() != null ? profile.getFitnessCoins() : 0;
        int lifetimeCoins = profile.getLifetimeCoinsEarned() != null ? profile.getLifetimeCoinsEarned() : 0;

        profile.setFitnessCoins(currentCoins + coins);
        profile.setLifetimeCoinsEarned(lifetimeCoins + coins);

        profileRepository.save(profile);

        log.info("💰 User {} earned {} coins", userId, coins);

        return buildCoinReward(coins, durationMinutes, personalRecordsAchieved, currentStreak);
    }

    /**
     * CLEAR FORMULA:
     * Base: 10 coins
     * Duration: 1 coin per 5 minutes (max 12)
     * PRs: 5 coins each (max 25)
     * Streak: 1 coin per day (max 30)
     */
    private int calculateWorkoutCoins(int durationMinutes, int prs, int streak) {
        int base = 10;
        int durationBonus = Math.min(durationMinutes / 5, 12);
        int prBonus = Math.min(prs * 5, 25);
        int streakBonus = Math.min(streak, 30);

        return base + durationBonus + prBonus + streakBonus;
    }

    /**
     * Spend coins for purchases
     */
    @Transactional
    public boolean spendCoins(Long userId, int amount, String itemId) {
        UserGamificationProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        int currentCoins = profile.getFitnessCoins() != null ? profile.getFitnessCoins() : 0;

        if (currentCoins < amount) {
            log.warn("User {} has insufficient coins: {} < {}", userId, currentCoins, amount);
            return false;
        }

        profile.setFitnessCoins(currentCoins - amount);
        profileRepository.save(profile);

        log.info("💸 User {} spent {} coins on '{}'", userId, amount, itemId);
        return true;
    }

    /**
     * Get user's coin balance
     */
    public int getBalance(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(profile -> profile.getFitnessCoins() != null ? profile.getFitnessCoins() : 0)
                .orElse(0);
    }

    /**
     * Award coins for challenge completion
     */
    @Transactional
    public void awardChallengeCoins(Long userId, int coins, String challengeId) {
        UserGamificationProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found: " + userId));

        int currentCoins = profile.getFitnessCoins() != null ? profile.getFitnessCoins() : 0;
        int lifetimeCoins = profile.getLifetimeCoinsEarned() != null ? profile.getLifetimeCoinsEarned() : 0;

        profile.setFitnessCoins(currentCoins + coins);
        profile.setLifetimeCoinsEarned(lifetimeCoins + coins);

        profileRepository.save(profile);

        log.info("🏆 User {} earned {} coins from challenge '{}'", userId, coins, challengeId);
    }

    private CoinReward buildCoinReward(int total, int duration, int prs, int streak) {
        return CoinReward.builder()
                .totalCoins(total)
                .baseCoins(10)
                .durationBonus(Math.min(duration / 5, 12))
                .personalRecordBonus(Math.min(prs * 5, 25))
                .streakBonus(Math.min(streak, 30))
                .build();
    }

    @Data
    @Builder
    public static class CoinReward {
        private Integer totalCoins;
        private Integer baseCoins;
        private Integer durationBonus;
        private Integer personalRecordBonus;
        private Integer streakBonus;
    }
}