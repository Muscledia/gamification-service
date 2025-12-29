package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * PURPOSE: Migrate existing users without usernames
 * RESPONSIBILITY: Ensure all profiles have usernames
 * COUPLING: Low - only uses repository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsernameMigrationService implements CommandLineRunner {

    private final UserGamificationProfileRepository profileRepository;
    private final NameGeneratorService nameGenerator;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🔄 Migrating imaginary usernames to realistic names...");

        List<UserGamificationProfile> allProfiles = profileRepository.findAll();
        int updated = 0;

        for (UserGamificationProfile profile : allProfiles) {
            boolean needsUpdate = false;

            // Check if username needs updating
            if (nameGenerator.isGeneratedUsername(profile.getUsername())) {
                String newUsername = nameGenerator.generateUsername();
                String newDisplayName = nameGenerator.generateDisplayName();

                profile.setUsername(newUsername);
                profile.setUsername(newDisplayName);
                profile.setLastUpdated(Instant.now());

                needsUpdate = true;
                log.info("Updated user {} with username: '{}', displayName: '{}'",
                        profile.getUserId(), newUsername, newDisplayName);
            }
            // Check if displayName needs updating
            else if (profile.getUsername() == null || profile.getUsername().trim().isEmpty()) {
                String newDisplayName = nameGenerator.generateDisplayNameFromUsername(profile.getUsername());
                profile.setUsername(newDisplayName);
                profile.setLastUpdated(Instant.now());
                needsUpdate = true;
            }

            if (needsUpdate) {
                profileRepository.save(profile);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("✅ Migrated {} user profiles with realistic names", updated);
        } else {
            log.info("✅ All user profiles already have proper names");
        }
    }
}