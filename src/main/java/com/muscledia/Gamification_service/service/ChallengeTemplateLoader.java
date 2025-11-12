package com.muscledia.Gamification_service.service;


import com.muscledia.Gamification_service.dto.yaml.ChallengeTemplateYaml;
import com.muscledia.Gamification_service.dto.yaml.UserJourneyYaml;
import com.muscledia.Gamification_service.model.ChallengeTemplate;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.repository.ChallengeTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * PURPOSE: Load challenge templates from YAML files and convert to entities
 * RESPONSIBILITY: Parse YAML files and create ChallengeTemplate entities
 * COUPLING: Low - depends only on repository and file system
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "gamification.challenge-templates.auto-load", havingValue = "true", matchIfMissing = true)
public class ChallengeTemplateLoader {

    private final ChallengeTemplateRepository templateRepository;
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    // FIXED: Use proper Jackson configuration
    private final ObjectMapper yamlMapper = createYamlMapper();

    private ObjectMapper createYamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // Configure for flexibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return mapper;
    }

    @PostConstruct
    @Transactional
    public void loadAllTemplates() {
        log.info("🎯 Starting challenge template loading from YAML files");

        try {
            // Clear existing templates if reload is enabled
            if (shouldReload()) {
                log.info("Clearing existing templates for reload");
                templateRepository.deleteAll();
            }

            // Load challenge templates from all difficulty folders
            loadChallengeTemplates();

            // Load user journey configurations (optional - for reference)
            loadUserJourneyConfigurations();

            log.info("✅ Successfully loaded all challenge templates");

        } catch (Exception e) {
            log.error("❌ Failed to load challenge templates: {}", e.getMessage(), e);

            // DON'T throw exception to prevent startup failure
            log.warn("🔄 Continuing without challenge templates loaded");
        }
    }

    private void loadChallengeTemplates() throws IOException {
        log.info("Loading challenge templates from difficulty folders");

        // Load from each difficulty folder
        String[] difficultyFolders = {"beginner", "intermediate", "advanced"};
        int totalLoaded = 0;

        for (String folder : difficultyFolders) {
            try {
                String pattern = "classpath:challenge-progressions/" + folder + "/*.yml";
                Resource[] resources = resourceResolver.getResources(pattern);

                log.info("Found {} YAML files in {} folder", resources.length, folder);

                for (Resource resource : resources) {
                    try {
                        int loaded = loadSingleTemplateFile(resource, folder);
                        totalLoaded += loaded;
                    } catch (Exception e) {
                        log.error("Failed to load template file {}: {}", resource.getFilename(), e.getMessage());
                        // Continue with other files
                    }
                }
            } catch (IOException e) {
                log.warn("Could not load templates from {} folder: {}", folder, e.getMessage());
                // Continue with other folders
            }
        }

        log.info("Loaded {} challenge templates from all difficulty folders", totalLoaded);
    }

    private int loadSingleTemplateFile(Resource resource, String difficultyFolder) throws IOException {
        log.debug("Loading templates from: {}", resource.getFilename());

        try (InputStream inputStream = resource.getInputStream()) {

            // FIXED: Better error handling for YAML parsing
            ChallengeTemplateYaml yamlData;
            try {
                yamlData = yamlMapper.readValue(inputStream, ChallengeTemplateYaml.class);
            } catch (Exception e) {
                log.error("Failed to parse YAML file {}: {}", resource.getFilename(), e.getMessage());
                return 0;
            }

            if (yamlData.getTemplates() == null || yamlData.getTemplates().isEmpty()) {
                log.warn("No templates found in file: {}", resource.getFilename());
                return 0;
            }

            int loadedCount = 0;
            for (Map.Entry<String, ChallengeTemplateYaml.TemplateDefinition> entry :
                    yamlData.getTemplates().entrySet()) {

                try {
                    ChallengeTemplate template = convertToEntity(entry.getValue(), yamlData, difficultyFolder);

                    // Check if template already exists
                    if (templateRepository.existsById(template.getId())) {
                        log.debug("Template {} already exists, updating", template.getId());
                        // Update instead of skip
                    }

                    templateRepository.save(template);
                    loadedCount++;

                    log.debug("✅ Loaded template: {} ({})", template.getName(), template.getId());

                } catch (Exception e) {
                    log.error("Failed to convert template {}: {}", entry.getKey(), e.getMessage());
                }
            }

            log.info("Loaded {} templates from {}", loadedCount, resource.getFilename());
            return loadedCount;
        }
    }

    private ChallengeTemplate convertToEntity(ChallengeTemplateYaml.TemplateDefinition def,
                                              ChallengeTemplateYaml yamlData,
                                              String difficultyFolder) {

        ChallengeTemplate.ChallengeTemplateBuilder builder = ChallengeTemplate.builder()
                .id(def.getId())
                .name(def.getName())
                .description(def.getDescription())
                .type(parseChallengeType(def.getType()))
                .objective(parseObjectiveType(def.getObjective()))
                .active(true)
                .weight(1.0)
                .createdAt(Instant.now());

        // Set journey phase from YAML data or default from folder
        String journeyPhase = yamlData.getJourneyPhase() != null ?
                yamlData.getJourneyPhase() :
                mapFolderToPhase(difficultyFolder);
        builder.journeyPhase(journeyPhase);

        // Convert difficulty scaling to target/reward maps
        if (def.getDifficultyScaling() != null) {
            Map<DifficultyLevel, Integer> targetValues = new HashMap<>();
            Map<DifficultyLevel, Integer> rewardPoints = new HashMap<>();

            for (Map.Entry<String, ChallengeTemplateYaml.DifficultyScaling> entry :
                    def.getDifficultyScaling().entrySet()) {

                DifficultyLevel difficulty = parseDifficultyLevel(entry.getKey());
                ChallengeTemplateYaml.DifficultyScaling scaling = entry.getValue();

                if (scaling.getTarget() != null) {
                    targetValues.put(difficulty, scaling.getTarget());
                }
                if (scaling.getPoints() != null) {
                    rewardPoints.put(difficulty, scaling.getPoints());
                }
            }

            builder.targetValues(targetValues);
            builder.rewardPoints(rewardPoints);
        }

        // Set prerequisites and unlocks
        if (def.getPrerequisites() != null) {
            builder.prerequisiteTemplates(new ArrayList<>(def.getPrerequisites()));
        }

        if (def.getUnlocks() != null) {
            builder.unlocksTemplates(new ArrayList<>(def.getUnlocks()));
        }

        // Set journey tags
        if (def.getJourneyTags() != null) {
            builder.userJourneyTags(new HashSet<>(def.getJourneyTags()));
        }

        // Set metadata
        builder.metadata(createMetadata(def));

        return builder.build();
    }

    private Map<String, Object> createMetadata(ChallengeTemplateYaml.TemplateDefinition def) {
        Map<String, Object> metadata = new HashMap<>();

        if (def.getCompletionMessage() != null) {
            metadata.put("completionMessage", def.getCompletionMessage());
        }
        if (def.getMilestone() != null && def.getMilestone()) {
            metadata.put("milestone", true);
        }
        if (def.getLegendary() != null && def.getLegendary()) {
            metadata.put("legendary", true);
        }
        if (def.getExerciseFocus() != null) {
            metadata.put("exerciseFocus", def.getExerciseFocus());
        }
        if (def.getSafetyNote() != null) {
            metadata.put("safetyNote", def.getSafetyNote());
        }

        return metadata;
    }

    private String mapFolderToPhase(String folder) {
        return switch (folder.toLowerCase()) {
            case "beginner" -> "foundation";
            case "intermediate" -> "building";
            case "advanced" -> "mastery";
            default -> "foundation";
        };
    }

    private void loadUserJourneyConfigurations() {
        try {
            log.info("Loading user journey configurations");

            String pattern = "classpath:challenge-progressions/user-journeys/*.yml";
            Resource[] resources = resourceResolver.getResources(pattern);

            log.info("Found {} user journey configuration files", resources.length);

            for (Resource resource : resources) {
                try {
                    loadUserJourneyFile(resource);
                } catch (Exception e) {
                    log.error("Failed to load user journey file {}: {}",
                            resource.getFilename(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Could not load user journey configurations: {}", e.getMessage());
        }
    }

    private void loadUserJourneyFile(Resource resource) throws IOException {
        log.debug("Loading user journey from: {}", resource.getFilename());

        try (InputStream inputStream = resource.getInputStream()) {
            UserJourneyYaml journeyData = yamlMapper.readValue(inputStream, UserJourneyYaml.class);

            log.info("Loaded user journey: {} with {} phases",
                    journeyData.getName(),
                    journeyData.getPhases() != null ? journeyData.getPhases().size() : 0);
        }
    }

    // Helper methods for parsing enums
    private ChallengeType parseChallengeType(String type) {
        if (type == null) return ChallengeType.DAILY;

        try {
            return ChallengeType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown challenge type: {}, defaulting to DAILY", type);
            return ChallengeType.DAILY;
        }
    }

    private ObjectiveType parseObjectiveType(String objective) {
        if (objective == null) return ObjectiveType.EXERCISES;

        try {
            return ObjectiveType.valueOf(objective.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown objective type: {}, defaulting to EXERCISES", objective);
            return ObjectiveType.EXERCISES;
        }
    }

    private DifficultyLevel parseDifficultyLevel(String difficulty) {
        if (difficulty == null) return DifficultyLevel.BEGINNER;

        try {
            return DifficultyLevel.valueOf(difficulty.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown difficulty level: {}, defaulting to BEGINNER", difficulty);
            return DifficultyLevel.BEGINNER;
        }
    }

    private boolean shouldReload() {
        try {
            return templateRepository.count() == 0;
        } catch (Exception e) {
            log.warn("Could not check template count: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Manual reload method for development/admin use
     */
    @Transactional
    public void reloadTemplates() {
        log.info("🔄 Manually reloading challenge templates");
        templateRepository.deleteAll();
        loadAllTemplates();
    }

    /**
     * Get loading statistics
     */
    public Map<String, Object> getLoadingStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalTemplates", templateRepository.count());
            stats.put("lastLoadTime", Instant.now());

            // Count by journey phase
            Map<String, Long> phaseCount = new HashMap<>();
            List<ChallengeTemplate> allTemplates = templateRepository.findAll();

            for (ChallengeTemplate template : allTemplates) {
                String phase = template.getJourneyPhase() != null ? template.getJourneyPhase() : "unknown";
                phaseCount.merge(phase, 1L, Long::sum);
            }

            stats.put("templatesByPhase", phaseCount);
        } catch (Exception e) {
            log.error("Error getting loading statistics", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
