package com.muscledia.Gamification_service.model.enums;

import lombok.Getter;

@Getter
public enum ChallengeCategory {
    STRENGTH("Build muscle and power"),
    CARDIO("Improve endurance and burn calories"),
    CONSISTENCY("Build healthy habits"),
    SKILL("Master new techniques"),
    RECOVERY("Focus on rest and wellness"),
    SOCIAL("Community and sharing");

    private final String description;

    ChallengeCategory(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
