package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.request.QuestProgressRequest;
import com.muscledia.Gamification_service.dto.response.ApiResponse;
import com.muscledia.Gamification_service.model.Quest;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.UserQuestProgress;
import com.muscledia.Gamification_service.model.enums.QuestType;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.model.enums.QuestStatus;
import com.muscledia.Gamification_service.service.QuestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class QuestController {

    private final QuestService questService;

    /**
     * Create a new quest
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Quest>> createQuest(@Valid @RequestBody Quest quest) {
        log.info("Creating new quest: {}", quest.getName());

        try {
            Quest createdQuest = questService.createQuest(quest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Quest created successfully", createdQuest));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating quest", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create quest"));
        }
    }

    /**
     * Get active quests for a user
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<ApiResponse<List<Quest>>> getActiveQuestsForUser(@PathVariable Long userId) {
        log.info("Getting active quests for user {}", userId);

        try {
            List<Quest> activeQuests = questService.getActiveQuestsForUser(userId);
            return ResponseEntity.ok(ApiResponse.success(activeQuests));
        } catch (Exception e) {
            log.error("Error getting active quests for user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve active quests"));
        }
    }

    /**
     * Get all quests with optional filtering
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Quest>>> getAllQuests(
            @RequestParam(required = false) QuestType questType,
            @RequestParam(required = false) ObjectiveType objectiveType,
            @RequestParam(required = false) Integer userLevel) {

        log.info("Getting all quests with filters - questType: {}, objectiveType: {}, userLevel: {}",
                questType, objectiveType, userLevel);

        try {
            List<Quest> quests = questService.getAllQuests(questType, objectiveType, userLevel);
            return ResponseEntity.ok(ApiResponse.success(quests));
        } catch (Exception e) {
            log.error("Error getting quests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve quests"));
        }
    }

    /**
     * Start a quest for a user
     */
    @PostMapping("/{questId}/start/{userId}")
    public ResponseEntity<ApiResponse<UserQuestProgress>> startQuest(
            @PathVariable String questId,
            @PathVariable Long userId) {

        log.info("Starting quest {} for user {}", questId, userId);

        try {
            UserQuestProgress questProgress = questService.startQuest(userId, questId);
            return ResponseEntity.ok(ApiResponse.success("Quest started successfully", questProgress));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error starting quest", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to start quest"));
        }
    }

    /**
     * Update quest progress for a user
     */
    @PutMapping("/{questId}/progress")
    public ResponseEntity<ApiResponse<UserQuestProgress>> updateQuestProgress(
            @PathVariable String questId,
            @Valid @RequestBody QuestProgressRequest request) {

        log.info("Updating quest progress for quest {} and user {}", questId, request.getUserId());

        try {
            UserQuestProgress questProgress = questService.updateQuestProgress(
                    request.getUserId(), questId, request.getProgressIncrement());
            return ResponseEntity.ok(ApiResponse.success("Quest progress updated successfully", questProgress));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating quest progress", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update quest progress"));
        }
    }

    /**
     * Complete a quest for a user
     */
    @PostMapping("/{questId}/complete/{userId}")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> completeQuest(
            @PathVariable String questId,
            @PathVariable Long userId) {

        log.info("Completing quest {} for user {}", questId, userId);

        try {
            UserGamificationProfile updatedProfile = questService.completeQuest(userId, questId);
            return ResponseEntity.ok(ApiResponse.success("Quest completed successfully", updatedProfile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error completing quest", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to complete quest"));
        }
    }

    /**
     * Get user quest progress with optional status filtering
     */
    @GetMapping("/user/{userId}/progress")
    public ResponseEntity<ApiResponse<List<UserQuestProgress>>> getUserQuestProgress(
            @PathVariable Long userId,
            @RequestParam(required = false) QuestStatus status) {

        log.info("Getting quest progress for user {} with status {}", userId, status);

        try {
            List<UserQuestProgress> questProgress = questService.getUserQuestProgress(userId, status);
            return ResponseEntity.ok(ApiResponse.success(questProgress));
        } catch (Exception e) {
            log.error("Error getting user quest progress", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve quest progress"));
        }
    }

    /**
     * Get quests by type and difficulty
     */
    @GetMapping("/type-difficulty")
    public ResponseEntity<ApiResponse<List<Quest>>> getQuestsByTypeAndDifficulty(
            @RequestParam QuestType questType,
            @RequestParam @Min(value = 1, message = "Max level must be positive") int maxLevel) {

        log.info("Getting quests by type {} and max level {}", questType, maxLevel);

        try {
            List<Quest> quests = questService.getQuestsByTypeAndDifficulty(questType, maxLevel);
            return ResponseEntity.ok(ApiResponse.success(quests));
        } catch (Exception e) {
            log.error("Error getting quests by type and difficulty", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve quests by type and difficulty"));
        }
    }

    /**
     * Get upcoming quests
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Quest>>> getUpcomingQuests() {
        log.info("Getting upcoming quests");

        try {
            List<Quest> upcomingQuests = questService.getUpcomingQuests();
            return ResponseEntity.ok(ApiResponse.success(upcomingQuests));
        } catch (Exception e) {
            log.error("Error getting upcoming quests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve upcoming quests"));
        }
    }

    /**
     * Get expired quests
     */
    @GetMapping("/expired")
    public ResponseEntity<ApiResponse<List<Quest>>> getExpiredQuests() {
        log.info("Getting expired quests");

        try {
            List<Quest> expiredQuests = questService.getExpiredQuests();
            return ResponseEntity.ok(ApiResponse.success(expiredQuests));
        } catch (Exception e) {
            log.error("Error getting expired quests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve expired quests"));
        }
    }

    /**
     * Get quest statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuestStatistics() {
        log.info("Getting quest statistics");

        try {
            Map<String, Object> statistics = questService.getQuestStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("Error getting quest statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve quest statistics"));
        }
    }

    /**
     * Delete a quest
     */
    @DeleteMapping("/{questId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuest(@PathVariable String questId) {
        log.info("Deleting quest {}", questId);

        try {
            questService.deleteQuest(questId);
            return ResponseEntity.ok(ApiResponse.success("Quest deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting quest", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete quest"));
        }
    }

    /**
     * Process expired quests (admin operation)
     */
    @PostMapping("/process-expired")
    public ResponseEntity<ApiResponse<Void>> processExpiredQuests() {
        log.info("Processing expired quests");

        try {
            questService.processExpiredQuests();
            return ResponseEntity.ok(ApiResponse.success("Expired quests processed successfully", null));
        } catch (Exception e) {
            log.error("Error processing expired quests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process expired quests"));
        }
    }

    /**
     * Generate scheduled quests (admin operation)
     */
    @PostMapping("/generate-scheduled")
    public ResponseEntity<ApiResponse<List<Quest>>> generateScheduledQuests() {
        log.info("Generating scheduled quests");

        try {
            List<Quest> generatedQuests = questService.generateScheduledQuests();
            return ResponseEntity.ok(ApiResponse.success("Scheduled quests generated successfully", generatedQuests));
        } catch (Exception e) {
            log.error("Error generating scheduled quests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate scheduled quests"));
        }
    }
}