package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserBadge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for handling welcome achievements when users register
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeAchievementService {

    private final UserGamificationProfileRepository profileRepository;

    @Transactional
    public void awardWelcomeAchievement(UserGamificationProfile profile) {
        try {
            log.info("Awarding welcome achievement to user {}", profile.getUserId());

            // Create welcome badge
            UserBadge welcomeBadge = UserBadge.builder()
                    .badgeId("WELCOME")
                    .badgeName("Welcome to Muscledia!")
                    .description("Welcome to your fitness journey!")
                    .category("welcome")
                    .pointsAwarded(10)
                    .earnedAt(Instant.now())
                    .build();

            // Add badge to profile
            profile.addBadge(welcomeBadge);

            // Award welcome points
            profile.setPoints(profile.getPoints() + 10);

            // Save the updated profile
            profileRepository.save(profile);

            log.info("Welcome achievement awarded to user {}", profile.getUserId());

        } catch (Exception e) {
            log.error("Failed to award welcome achievement to user {}: {}",
                    profile.getUserId(), e.getMessage(), e);
        }
    }

    @Transactional
    public void awardFirstLoginAchievement(Long userId) {
        try {
            UserGamificationProfile profile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Profile not found for user: " + userId));

            // Check if already has first login badge
            boolean hasFirstLogin = profile.getEarnedBadges().stream()
                    .anyMatch(badge -> "FIRST_LOGIN".equals(badge.getBadgeId()));

            if (!hasFirstLogin) {
                UserBadge firstLoginBadge = UserBadge.builder()
                        .badgeId("FIRST_LOGIN")
                        .badgeName("First Steps")
                        .description("Logged in for the first time!")
                        .category("milestone")
                        .pointsAwarded(5)
                        .earnedAt(Instant.now())
                        .build();

                profile.addBadge(firstLoginBadge);
                profile.setPoints(profile.getPoints() + 5);
                profileRepository.save(profile);

                log.info("First login achievement awarded to user {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to award first login achievement to user {}: {}",
                    userId, e.getMessage(), e);
        }
    }
}
