package com.muscledia.Gamification_service.dto.yaml;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class UserJourneyYaml {
    private String journeyId;
    private String name;
    private String description;
    private Map<String, JourneyPhase> phases;
    private Set<String> preferredChallengeTypes;
    private Set<String> preferredObjectives;
    private Double progressionMultiplier;

    @Data
    public static class JourneyPhase {
        private String name;
        private List<Integer> levelRange;
        private Integer durationWeeks;
        private String description;
        private List<String> keyObjectives;
        private List<String> successCriteria;
    }
}
