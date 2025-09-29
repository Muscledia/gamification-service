package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.request.UserStatsRequest;
import com.muscledia.Gamification_service.dto.response.ApiResponse;
import com.muscledia.Gamification_service.model.Champion;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.enums.ChampionCriteriaType;
import com.muscledia.Gamification_service.service.ChampionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/champions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
public class ChampionController {

    private final ChampionService championService;

    /**
     * Create a new champion
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Champion>> createChampion(@Valid @RequestBody Champion champion) {
        log.info("Creating new champion: {}", champion.getName());

        try {
            Champion createdChampion = championService.createChampion(champion);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Champion created successfully", createdChampion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating champion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create champion"));
        }
    }

    /**
     * Get all champions with optional filtering
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Champion>>> getAllChampions(
            @RequestParam(required = false) ChampionCriteriaType criteriaType,
            @RequestParam(required = false) Integer maxDifficulty,
            @RequestParam(required = false) String exerciseId,
            @RequestParam(required = false) String muscleGroupId) {

        log.info(
                "Getting all champions with filters - criteriaType: {}, maxDifficulty: {}, exerciseId: {}, muscleGroupId: {}",
                criteriaType, maxDifficulty, exerciseId, muscleGroupId);

        try {
            List<Champion> champions = championService.getAllChampions(criteriaType, maxDifficulty, exerciseId,
                    muscleGroupId);
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting champions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champions"));
        }
    }

    /**
     * Get champions by specific difficulty
     */
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<ApiResponse<List<Champion>>> getChampionsByDifficulty(
            @PathVariable @Min(1) @Max(10) int difficulty) {

        log.info("Getting champions with difficulty {}", difficulty);

        try {
            List<Champion> champions = championService.getChampionsByDifficulty(difficulty);
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting champions by difficulty", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champions by difficulty"));
        }
    }

    /**
     * Get champions by difficulty range
     */
    @GetMapping("/difficulty-range")
    public ResponseEntity<ApiResponse<List<Champion>>> getChampionsByDifficultyRange(
            @RequestParam @Min(1) @Max(10) int minDifficulty,
            @RequestParam @Min(1) @Max(10) int maxDifficulty) {

        log.info("Getting champions with difficulty range {} to {}", minDifficulty, maxDifficulty);

        try {
            List<Champion> champions = championService.getChampionsByDifficultyRange(minDifficulty, maxDifficulty);
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting champions by difficulty range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champions by difficulty range"));
        }
    }

    /**
     * Get champions for a specific exercise
     */
    @GetMapping("/exercise/{exerciseId}")
    public ResponseEntity<ApiResponse<List<Champion>>> getChampionsForExercise(
            @PathVariable String exerciseId,
            @RequestParam(required = false) Integer maxDifficulty) {

        log.info("Getting champions for exercise {} with max difficulty {}", exerciseId, maxDifficulty);

        try {
            List<Champion> champions = championService.getChampionsForExercise(exerciseId, maxDifficulty);
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting champions for exercise", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champions for exercise"));
        }
    }

    /**
     * Get champions for a specific muscle group
     */
    @GetMapping("/muscle-group/{muscleGroupId}")
    public ResponseEntity<ApiResponse<List<Champion>>> getChampionsForMuscleGroup(
            @PathVariable String muscleGroupId,
            @RequestParam(required = false) Integer maxDifficulty) {

        log.info("Getting champions for muscle group {} with max difficulty {}", muscleGroupId, maxDifficulty);

        try {
            List<Champion> champions = championService.getChampionsForMuscleGroup(muscleGroupId, maxDifficulty);
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting champions for muscle group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champions for muscle group"));
        }
    }

    /**
     * Get general champions (not exercise-specific)
     */
    @GetMapping("/general")
    public ResponseEntity<ApiResponse<List<Champion>>> getGeneralChampions() {
        log.info("Getting general champions");

        try {
            List<Champion> champions = championService.getGeneralChampions();
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting general champions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve general champions"));
        }
    }

    /**
     * Get exercise-specific champions
     */
    @GetMapping("/exercise-specific")
    public ResponseEntity<ApiResponse<List<Champion>>> getExerciseSpecificChampions() {
        log.info("Getting exercise-specific champions");

        try {
            List<Champion> champions = championService.getExerciseSpecificChampions();
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting exercise-specific champions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve exercise-specific champions"));
        }
    }

    /**
     * Check if user meets champion criteria
     */
    @PostMapping("/{championId}/check-criteria")
    public ResponseEntity<ApiResponse<Boolean>> checkChampionCriteria(
            @PathVariable String championId,
            @Valid @RequestBody UserStatsRequest request) {

        log.info("Checking champion criteria for champion {} and user {}", championId, request.getUserId());

        try {
            boolean meets = championService.checkChampionCriteria(request.getUserId(), championId,
                    request.getUserStats());
            String message = meets ? "User meets champion criteria" : "User does not meet champion criteria";
            return ResponseEntity.ok(ApiResponse.success(message, meets));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error checking champion criteria", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check champion criteria"));
        }
    }

    /**
     * Get eligible champions for a user based on their stats
     */
    @PostMapping("/eligible")
    public ResponseEntity<ApiResponse<List<Champion>>> getEligibleChampions(
            @Valid @RequestBody UserStatsRequest request) {

        log.info("Getting eligible champions for user {}", request.getUserId());

        try {
            List<Champion> eligibleChampions = championService.getEligibleChampions(request.getUserId(),
                    request.getUserStats());
            return ResponseEntity.ok(ApiResponse.success(eligibleChampions));
        } catch (Exception e) {
            log.error("Error getting eligible champions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get eligible champions"));
        }
    }

    /**
     * Award a champion to a user
     */
    @PostMapping("/{championId}/award/{userId}")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> awardChampion(
            @PathVariable String championId,
            @PathVariable Long userId) {

        log.info("Awarding champion {} to user {}", championId, userId);

        try {
            UserGamificationProfile updatedProfile = championService.awardChampion(userId, championId);
            return ResponseEntity.ok(ApiResponse.success("Champion awarded successfully", updatedProfile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error awarding champion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to award champion"));
        }
    }

    /**
     * Get champion statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChampionStatistics() {
        log.info("Getting champion statistics");

        try {
            Map<String, Object> statistics = championService.getChampionStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("Error getting champion statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champion statistics"));
        }
    }

    /**
     * Get champions ordered by difficulty (ascending)
     */
    @GetMapping("/difficulty/ascending")
    public ResponseEntity<ApiResponse<List<Champion>>> getChampionsByDifficultyAscending() {
        log.info("Getting champions by difficulty ascending");

        try {
            List<Champion> champions = championService.getChampionsByDifficultyAscending();
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting champions by difficulty ascending", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champions by difficulty ascending"));
        }
    }

    /**
     * Get champions ordered by difficulty (descending)
     */
    @GetMapping("/difficulty/descending")
    public ResponseEntity<ApiResponse<List<Champion>>> getChampionsByDifficultyDescending() {
        log.info("Getting champions by difficulty descending");

        try {
            List<Champion> champions = championService.getChampionsByDifficultyDescending();
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting champions by difficulty descending", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve champions by difficulty descending"));
        }
    }

    /**
     * Get recently created champions
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<Champion>>> getRecentlyCreatedChampions() {
        log.info("Getting recently created champions");

        try {
            List<Champion> champions = championService.getRecentlyCreatedChampions();
            return ResponseEntity.ok(ApiResponse.success(champions));
        } catch (Exception e) {
            log.error("Error getting recently created champions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve recently created champions"));
        }
    }

    /**
     * Update a champion
     */
    @PutMapping("/{championId}")
    public ResponseEntity<ApiResponse<Champion>> updateChampion(
            @PathVariable String championId,
            @Valid @RequestBody Champion championUpdates) {

        log.info("Updating champion {}", championId);

        try {
            Champion updatedChampion = championService.updateChampion(championId, championUpdates);
            return ResponseEntity.ok(ApiResponse.success("Champion updated successfully", updatedChampion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating champion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update champion"));
        }
    }

    /**
     * Delete a champion
     */
    @DeleteMapping("/{championId}")
    public ResponseEntity<ApiResponse<Void>> deleteChampion(@PathVariable String championId) {
        log.info("Deleting champion {}", championId);

        try {
            championService.deleteChampion(championId);
            return ResponseEntity.ok(ApiResponse.success("Champion deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting champion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete champion"));
        }
    }
}