package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.event.UserRegisteredEvent;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Specialized service for creating user profiles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileCreationService {
    private final UserGamificationProfileRepository profileRepository;
    private final NameGeneratorService nameGenerator;

    public UserGamificationProfile createProfile(UserRegisteredEvent event) {
        log.info("Creating gamification profile for user {}", event.getUserId());

        String username = event.getUsername();
        String displayName = null;

        if (username == null || username.trim().isEmpty() || username.startsWith("User#")) {
            // Generate realistic username for imaginary users
            username = nameGenerator.generateUsername();
            displayName = nameGenerator.generateDisplayName();
            log.info("Generated imaginary username '{}' and displayName '{}' for user {}",
                    username, displayName, event.getUserId());
        } else {
            // Real user - extract display name from event or use username
            displayName = extractDisplayName(event);
        }

        UserGamificationProfile profile = UserGamificationProfile.builder()
                .userId(event.getUserId())
                .username(username)
                .points(0)
                .level(1)
                .fitnessCoins(0)
                .lifetimeCoinsEarned(0)
                .totalWorkoutsCompleted(0)
                .weeklyStreak(0)
                .longestWeeklyStreak(0)
                .monthlyStreak(0)
                .longestMonthlyStreak(0)
                .restDaysSinceLastWorkout(0)
                .profileCreatedAt(Instant.now())
                .lastUpdated(Instant.now())
                .build();

        profile.initializeDefaults();
        return profileRepository.save(profile);
    }

    public UserGamificationProfile createDefaultProfile(Long userId) {
        log.debug("Creating default profile for user {}", userId);

        // ⬅️ GENERATE NAMES FOR DEFAULT PROFILES TOO
        String username = nameGenerator.generateUsername();
        String displayName = nameGenerator.generateDisplayName();

        UserGamificationProfile profile = UserGamificationProfile.builder()
                .userId(userId)
                .username(username)
                .points(0)
                .level(1)
                .fitnessCoins(0)
                .lifetimeCoinsEarned(0)
                .totalWorkoutsCompleted(0)
                .weeklyStreak(0)
                .longestWeeklyStreak(0)
                .monthlyStreak(0)
                .longestMonthlyStreak(0)
                .restDaysSinceLastWorkout(0)
                .profileCreatedAt(Instant.now())
                .lastUpdated(Instant.now())
                .build();

        profile.initializeDefaults();
        return profileRepository.save(profile);
    }

    private String extractDisplayName(UserRegisteredEvent event) {
        // Try to get display name from userPreferences
        if (event.getUserPreferences() != null) {
            Object displayNameObj = event.getUserPreferences().get("displayName");
            if (displayNameObj != null) {
                return displayNameObj.toString();
            }
        }

        // Fallback to username
        return event.getUsername() != null ? event.getUsername() : nameGenerator.generateDisplayName();
    }

    private void applyRegistrationPreferences(UserGamificationProfile profile, UserRegisteredEvent event) {
        if (event.getUserPreferences() != null) {
            if (event.getGoalType() != null) {
                log.debug("Applying goal type: {} for user {}", event.getGoalType(), event.getUserId());
                // Apply goal-specific preferences
            }
            if (event.getInitialAvatarType() != null) {
                log.debug("Initial avatar type: {} for user {}", event.getInitialAvatarType(), event.getUserId());
                // Apply avatar preferences
            }
        }
    }
}
