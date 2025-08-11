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

    public UserGamificationProfile createProfile(UserRegisteredEvent event) {
        log.debug("Creating profile from registration event for user {}", event.getUserId());

        UserGamificationProfile profile = UserGamificationProfile.builder()
                .userId(event.getUserId())
                .points(0)
                .level(1)
                .totalWorkoutsCompleted(0)
                .lastLevelUpDate(Instant.now())
                .streaks(new HashMap<>())
                .earnedBadges(new ArrayList<>())
                .quests(new ArrayList<>())
                .profileCreatedAt(event.getRegistrationDate())
                .lastUpdated(Instant.now())
                .build();

        // Apply user preferences from registration
        applyRegistrationPreferences(profile, event);

        // Initialize default values
        profile.initializeDefaults();

        return profileRepository.save(profile);
    }

    public UserGamificationProfile createDefaultProfile(Long userId) {
        log.debug("Creating default profile for user {}", userId);

        UserGamificationProfile profile = UserGamificationProfile.builder()
                .userId(userId)
                .points(0)
                .level(1)
                .totalWorkoutsCompleted(0)
                .lastLevelUpDate(Instant.now())
                .streaks(new HashMap<>())
                .earnedBadges(new ArrayList<>())
                .quests(new ArrayList<>())
                .profileCreatedAt(Instant.now())
                .lastUpdated(Instant.now())
                .build();

        profile.initializeDefaults();
        return profileRepository.save(profile);
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
