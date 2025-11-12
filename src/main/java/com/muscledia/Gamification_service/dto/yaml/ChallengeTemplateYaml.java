package com.muscledia.Gamification_service.dto.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChallengeTemplateYaml {

    @JsonProperty("template_group")
    private String templateGroup;

    @JsonProperty("journey_phase")
    private String journeyPhase;

    private String category;
    private Map<String, TemplateDefinition> templates;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplateDefinition {
        private String id;
        private String name;
        private String description;
        private String type;
        private String objective;

        @JsonProperty("difficulty_scaling")
        private Map<String, DifficultyScaling> difficultyScaling;

        @JsonProperty("duration_days")
        private Integer durationDays;

        @JsonProperty("journey_tags")
        private Set<String> journeyTags;

        private List<String> prerequisites;
        private List<String> unlocks;

        @JsonProperty("auto_enroll")
        private Boolean autoEnroll;

        @JsonProperty("completion_message")
        private String completionMessage;

        private Boolean milestone;
        private Boolean legendary;

        @JsonProperty("measurement_unit")
        private String measurementUnit;

        @JsonProperty("exercise_focus")
        private List<String> exerciseFocus;

        @JsonProperty("safety_note")
        private String safetyNote;

        @JsonProperty("quality_focus")
        private Boolean qualityFocus;

        @JsonProperty("technical_focus")
        private Boolean technicalFocus;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DifficultyScaling {
        private Integer target;
        private Integer points;
        private Integer exp;
    }
}
