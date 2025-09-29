package com.muscledia.Gamification_service.service.profile;

/**
 * Utility class for level calculations
 */
public class LevelCalculator {
    private LevelCalculator() {} // Utility class

    public static int calculateLevel(int points) {
        if (points < 100) return 1;
        if (points < 300) return 2;
        if (points < 600) return 3;
        if (points < 1000) return 4;
        if (points < 1500) return 5;
        if (points < 2100) return 6;
        if (points < 2800) return 7;
        if (points < 3600) return 8;
        if (points < 4500) return 9;
        return 10 + (points - 4500) / 1000;
    }

    public static int getPointsForLevel(int level) {
        if (level <= 1) return 0;
        if (level == 2) return 100;
        if (level == 3) return 300;
        if (level == 4) return 600;
        if (level == 5) return 1000;
        if (level == 6) return 1500;
        if (level == 7) return 2100;
        if (level == 8) return 2800;
        if (level == 9) return 3600;
        if (level == 10) return 4500;
        return 4500 + (level - 10) * 1000;
    }

    public static int getPointsToNextLevel(int currentPoints) {
        int currentLevel = calculateLevel(currentPoints);
        int nextLevelPoints = getPointsForLevel(currentLevel + 1);
        return nextLevelPoints - currentPoints;
    }
}
