package com.muscledia.Gamification_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Complete challenge catalog for frontend
 * Organized by category and journey phase
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeCatalogResponse {

    // Active challenges for this user
    private List<ChallengeResponse> activeChallenges;

    // Available challenges grouped by phase (foundation, building, mastery)
    private Map<String, List<ChallengeTemplateResponse>> availableChallenges;

    // Recommended challenges based on user journey
    private List<ChallengeTemplateResponse> recommendedChallenges;

    // Completed challenges (last 10)
    private List<ChallengeResponse> completedChallenges;

    // User journey info
    private UserJourneyInfo journeyInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserJourneyInfo {
        private String currentPhase;         // "foundation", "building", "mastery"
        private Integer currentLevel;
        private String primaryGoal;          // "strength_focused", "weight_loss", etc.
        private List<String> activeJourneyTags;
        private Integer completedChallenges;
        private Integer totalChallenges;
    }
}