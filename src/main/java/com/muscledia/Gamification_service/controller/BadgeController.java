package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.request.UserStatsRequest;
import com.muscledia.Gamification_service.dto.response.ApiResponse;
import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import com.muscledia.Gamification_service.service.BadgeService;
import com.muscledia.Gamification_service.utils.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;

/**
 * Badge Controller - Now with working Swagger documentation
 */
@RestController
@RequestMapping("/api/badges")
@Slf4j
@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@ConditionalOnProperty(value = "gamification.mongodb.enabled", havingValue = "true")
@Tag(name = "Badge Management", description = "Operations for managing badges, awarding them to users, and checking criteria")
public class BadgeController {

    private final BadgeService badgeService;


    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
        log.info("BadgeController SUCCESSFULLY LOADED!");
        log.info("Base path: /api/badges");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("Health check endpoint hit!");
        return ResponseEntity.ok("Badge Controller is loaded!");
    }

    /**
     * Create a new badge
     */
    @PostMapping
    @Operation(summary = "Create a new badge", description = "Creates a new badge with specified criteria and rewards. Badge names must be unique.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Badge created successfully", content = @Content(schema = @Schema(implementation = Badge.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or badge already exists", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Badge>> createBadge(
            @Parameter(description = "Badge object to create", required = true) @Valid @RequestBody Badge badge) {

        log.info("Creating new badge: {}", badge.getName());

        try {
            Badge createdBadge = badgeService.createBadge(badge);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Badge created successfully", createdBadge));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating badge", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create badge"));
        }
    }

    /**
     * Get all badges with optional filtering
     */
    @GetMapping
    @Operation(summary = "Get all badges", description = "Retrieves all badges with optional filtering by badge type and criteria type")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Badges retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to retrieve badges")
    })
    public ResponseEntity<ApiResponse<List<Badge>>> getAllBadges(
            @Parameter(description = "Filter by badge type") @RequestParam(required = false) BadgeType badgeType,
            @Parameter(description = "Filter by criteria type") @RequestParam(required = false) BadgeCriteriaType criteriaType) {

        log.info("Getting all badges with filters - badgeType: {}, criteriaType: {}", null, null); // Removed filters
                                                                                                   // for MVP

        try {
            List<Badge> badges = badgeService.getAllBadges(null, null); // Removed filters for MVP
            return ResponseEntity.ok(ApiResponse.success(badges));
        } catch (Exception e) {
            log.error("Error getting badges", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve badges"));
        }
    }

    /**
     * Get badges by minimum points requirement
     */
    @GetMapping("/min-points/{minPoints}")
    public ResponseEntity<ApiResponse<List<Badge>>> getBadgesByMinPoints(
            @PathVariable @Min(value = 0, message = "Minimum points must be non-negative") int minPoints) {

        log.info("Getting badges with minimum points: {}", minPoints);

        try {
            List<Badge> badges = badgeService.getBadgesByMinPoints(minPoints);
            return ResponseEntity.ok(ApiResponse.success(badges));
        } catch (Exception e) {
            log.error("Error getting badges by min points", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve badges by minimum points"));
        }
    }

    /**
     * Award a badge to a user
     */
    @PostMapping("/{badgeId}/award/{userId}")
    public ResponseEntity<ApiResponse<UserGamificationProfile>> awardBadge(
            @PathVariable String badgeId,
            @PathVariable Long userId) {

        log.info("Awarding badge {} to user {}", badgeId, userId);

        try {
            UserGamificationProfile updatedProfile = badgeService.awardBadge(userId, badgeId);
            return ResponseEntity.ok(ApiResponse.success("Badge awarded successfully", updatedProfile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error awarding badge", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to award badge"));
        }
    }

    /**
     * Check if user meets badge criteria
     */
    @PostMapping("/{badgeId}/check-criteria")
    public ResponseEntity<ApiResponse<Boolean>> checkBadgeCriteria(
            @PathVariable String badgeId,
            @Valid @RequestBody UserStatsRequest request) {

        log.info("Checking badge criteria for badge {} and user {}", badgeId, request.getUserId());

        try {
            boolean meets = badgeService.checkBadgeCriteria(request.getUserId(), badgeId, request.getUserStats());
            String message = meets ? "User meets badge criteria" : "User does not meet badge criteria";
            return ResponseEntity.ok(ApiResponse.success(message, meets));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error checking badge criteria", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check badge criteria"));
        }
    }

    /**
     * Get eligible badges for current user
     */
    @PostMapping("/eligible")
    public ResponseEntity<ApiResponse<List<Badge>>> getEligibleBadges(@Valid @RequestBody UserStatsRequest request) {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting eligible badges for current user {}", userId);

        // Override any userId in request with current user
        request.setUserId(userId);

        try {
            List<Badge> eligibleBadges = badgeService.getEligibleBadges(userId, request.getUserStats());
            return ResponseEntity.ok(ApiResponse.success(eligibleBadges));
        } catch (Exception e) {
            log.error("Error getting eligible badges", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get eligible badges"));
        }
    }

    /**
     * Get all badges earned by current user
     */
    @GetMapping("/my-badges")
    public ResponseEntity<ApiResponse<List<Badge>>> getMyBadges() {
        Long userId = AuthenticationService.getCurrentUserId();
        log.info("Getting badges for current user {}", userId);

        try {
            List<Badge> userBadges = badgeService.getUserBadges(userId);
            return ResponseEntity.ok(ApiResponse.success(userBadges));
        } catch (Exception e) {
            log.error("Error getting user badges", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve user badges"));
        }
    }

    /**
     * Get badge statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBadgeStatistics() {

        log.info("Getting badge statistics");

        try {
            Map<String, Object> statistics = badgeService.getBadgeStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("Error getting badge statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve badge statistics"));
        }
    }



    /**
     * Delete a badge
     */
    @DeleteMapping("/{badgeId}")
    public ResponseEntity<ApiResponse<Void>> deleteBadge(@PathVariable String badgeId) {

        log.info("Deleting badge {}", badgeId);

        try {
            badgeService.deleteBadge(badgeId);
            return ResponseEntity.ok(ApiResponse.success("Badge deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting badge", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete badge"));
        }
    }

}