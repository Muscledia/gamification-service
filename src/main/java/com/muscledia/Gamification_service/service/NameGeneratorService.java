package com.muscledia.Gamification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * PURPOSE: Generate realistic usernames and display names for imaginary users
 * RESPONSIBILITY: Provide random but believable user identities
 * COUPLING: None - standalone utility service
 */
@Service
@Slf4j
public class NameGeneratorService {

    private static final Random RANDOM = new Random();

    private static final List<String> FIRST_NAMES = List.of(
            "Alex", "Jordan", "Casey", "Morgan", "Taylor", "Riley", "Jamie", "Avery",
            "Quinn", "Sage", "Kai", "River", "Skylar", "Phoenix", "Dakota", "Cameron",
            "Dylan", "Logan", "Parker", "Reese", "Blake", "Drew", "Finley", "Rowan",
            "Sam", "Charlie", "Max", "Chris", "Pat", "Robin", "Jesse", "Frankie",
            "Angel", "Ash", "Bay", "Blair", "Eden", "Ellis", "Emery", "Gray",
            "Harley", "Haven", "Indigo", "Jules", "Justice", "Lane", "Lennon", "Lou",
            "Marley", "Monroe", "Nova", "Oakley", "Ocean", "Ari", "Atlas", "August",
            "Brooklyn", "Ezra", "Jaden", "Kendall", "Micah", "Nico", "Peyton", "Reign"
    );

    private static final List<String> FITNESS_ADJECTIVES = List.of(
            "Mighty", "Strong", "Swift", "Iron", "Steel", "Thunder", "Lightning", "Power",
            "Alpha", "Titan", "Warrior", "Champion", "Flex", "Beast", "Savage", "Elite",
            "Pro", "Fit", "Athletic", "Ripped", "Jacked", "Shredded", "Buff", "Swole",
            "Legendary", "Epic", "Unstoppable", "Fierce", "Bold", "Brave", "Dynamic", "Hardcore"
    );

    private static final List<String> FITNESS_NOUNS = List.of(
            "Lifter", "Athlete", "Crusher", "Grinder", "Pumper", "Builder", "Shredder", "Machine",
            "Force", "Tank", "Bull", "Lion", "Wolf", "Eagle", "Dragon", "Phoenix",
            "Gains", "Muscle", "Power", "Energy", "Vibe", "Flow", "Rush", "Surge",
            "Legend", "Hero", "Champ", "Star", "Ace", "Boss", "King", "Queen"
    );

    /**
     * Generate a random fitness-themed username
     */
    public String generateUsername() {
        int variant = RANDOM.nextInt(4);

        return switch (variant) {
            case 0 -> // FirstName + Number (e.g., "Alex2024")
                    randomFirstName() + randomNumber(2000, 2025);

            case 1 -> // Adjective + Noun (e.g., "MightyLifter")
                    randomAdjective() + randomNoun();

            case 2 -> // FirstName + Noun (e.g., "JordanCrusher")
                    randomFirstName() + randomNoun();

            default -> // Adjective + FirstName (e.g., "SwiftKai")
                    randomAdjective() + randomFirstName();
        };
    }

    /**
     * Generate a random display name (more human-readable)
     */
    public String generateDisplayName() {
        int variant = RANDOM.nextInt(3);

        return switch (variant) {
            case 0 -> // FirstName LastInitial (e.g., "Alex M.")
                    randomFirstName() + " " + randomLetter() + ".";

            case 1 -> // FirstName "The" Adjective (e.g., "Jordan The Mighty")
                    randomFirstName() + " The " + randomAdjective();

            default -> // FirstName + Adjective + Noun (e.g., "Casey Swift Warrior")
                    randomFirstName() + " " + randomAdjective() + " " + randomNoun();
        };
    }

    /**
     * Generate a simple display name from username
     */
    public String generateDisplayNameFromUsername(String username) {
        if (username == null || username.startsWith("User#")) {
            return generateDisplayName();
        }

        // If username is like "AlexLifter", make it "Alex The Lifter"
        String spacedName = username.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spacedName;
    }

    // Helper methods
    private String randomFirstName() {
        return FIRST_NAMES.get(RANDOM.nextInt(FIRST_NAMES.size()));
    }

    private String randomAdjective() {
        return FITNESS_ADJECTIVES.get(RANDOM.nextInt(FITNESS_ADJECTIVES.size()));
    }

    private String randomNoun() {
        return FITNESS_NOUNS.get(RANDOM.nextInt(FITNESS_NOUNS.size()));
    }

    private String randomLetter() {
        char letter = (char) ('A' + RANDOM.nextInt(26));
        return String.valueOf(letter);
    }

    private int randomNumber(int min, int max) {
        return min + RANDOM.nextInt(max - min + 1);
    }

    /**
     * Check if username is auto-generated (needs replacement)
     */
    public boolean isGeneratedUsername(String username) {
        return username == null ||
                username.startsWith("User#") ||
                username.matches("^User#\\d+$");
    }
}