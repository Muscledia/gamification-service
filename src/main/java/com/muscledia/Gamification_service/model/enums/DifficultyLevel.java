package com.muscledia.Gamification_service.model.enums;

import lombok.Getter;

@Getter
public enum DifficultyLevel {
    BEGINNER(1, "Perfect for getting started"),
    INTERMEDIATE(2, "Ready for more challenge"),
    ADVANCED(3, "Push your limits"),
    ELITE(4, "For the dedicated athletes");

    private final int level;
    private final String description;

    DifficultyLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    public static DifficultyLevel fromUserLevel(int userLevel) {
        if (userLevel <= 10) return BEGINNER;
        if (userLevel <= 25) return INTERMEDIATE;
        if (userLevel <= 50) return ADVANCED;
        return ELITE;
    }
}
