package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.request.ChallengeDto;
import com.muscledia.Gamification_service.dto.request.UserChallengeDto;
import com.muscledia.Gamification_service.dto.response.ApiResponse;
import com.muscledia.Gamification_service.dto.response.ChallengeCatalogResponse;
import com.muscledia.Gamification_service.dto.response.ChallengeResponse;
import com.muscledia.Gamification_service.dto.response.ChallengeTemplateResponse;
import com.muscledia.Gamification_service.mapper.ChallengeMapper;
import com.muscledia.Gamification_service.mapper.UserChallengeMapper;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.service.ChallengeService;
import com.muscledia.Gamification_service.utils.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PURPOSE: Challenge management REST endpoints
 * RESPONSIBILITY: Handle HTTP requests for challenge operations
 * COUPLING: Low - delegates to service layer
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ChallengeMapper challengeMapper;
    private final UserChallengeMapper userChallengeMapper;

    // ========== COMPREHENSIVE ENDPOINTS (Signal-Optimized) ==========

    /**
     * Get comprehensive challenge catalog for current user
     * Returns active, available, recommended, and completed challenges
     */
    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<ChallengeCatalogResponse>> getChallengeCatalog() {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting challenge catalog for user {}", userId);

            ChallengeCatalogResponse catalog = challengeService.getChallengeCatalog(userId);

            log.debug("Retrieved catalog with {} active, {} recommended challenges",
                    catalog.getActiveChallenges().size(),
                    catalog.getRecommendedChallenges().size());

            return ResponseEntity.ok(ApiResponse.success(
                    "Challenge catalog retrieved successfully", catalog));
        } catch (Exception e) {
            log.error("Error getting challenge catalog: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve challenge catalog"));
        }
    }

    /**
     * Get all available challenge templates with optional filtering
     * Query params: phase (foundation/building/mastery), category (strength/cardio/etc)
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<ChallengeTemplateResponse>>> getChallengeTemplates(
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String category) {

        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting challenge templates for user {} (phase: {}, category: {})",
                    userId, phase, category);

            List<ChallengeTemplateResponse> templates =
                    challengeService.getChallengeTemplates(userId, phase, category);

            log.debug("Found {} challenge templates", templates.size());

            return ResponseEntity.ok(ApiResponse.success(
                    "Challenge templates retrieved successfully", templates));
        } catch (Exception e) {
            log.error("Error getting challenge templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve challenge templates"));
        }
    }

    /**
     * Get detailed challenge information for a specific challenge
     */
    @GetMapping("/{challengeId}/details")
    public ResponseEntity<ApiResponse<ChallengeResponse>> getChallengeDetails(
            @PathVariable String challengeId) {

        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting challenge details: {} for user {}", challengeId, userId);

            ChallengeResponse challenge = challengeService.getChallengeDetails(userId, challengeId);
            return ResponseEntity.ok(ApiResponse.success(
                    "Challenge details retrieved successfully", challenge));
        } catch (IllegalArgumentException e) {
            log.warn("Challenge not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Challenge not found"));
        } catch (Exception e) {
            log.error("Error getting challenge details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve challenge details"));
        }
    }

    // ========== EXISTING ENDPOINTS (Refactored for Signal Optimization) ==========

    /**
     * Get available challenges by type with user progress
     * Uses Signal-optimized ChallengeDto
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getAvailableChallenges(
            @RequestParam(defaultValue = "DAILY") ChallengeType type) {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting {} challenges for user {}", type, userId);

            // Get challenges from service
            List<Challenge> challenges = challengeService.getAvailableChallenges(userId, type);

            // Get user's current challenges to show progress
            List<UserChallenge> userChallenges = challengeService.getActiveChallenges(userId);
            Map<String, UserChallenge> userChallengeMap = userChallenges.stream()
                    .collect(Collectors.toMap(UserChallenge::getChallengeId, uc -> uc));

            // Convert to DTOs with progress information
            List<ChallengeDto> dtos = challenges.stream()
                    .map(challenge -> {
                        UserChallenge userChallenge = userChallengeMap.get(challenge.getId());
                        return challengeMapper.toDto(challenge, userChallenge);
                    })
                    .collect(Collectors.toList());

            log.debug("Found {} available {} challenges", dtos.size(), type);

            return ResponseEntity.ok(ApiResponse.success(
                    type + " challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting available challenges: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve challenges"));
        }
    }

    /**
     * Get daily challenges (convenience endpoint)
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getDailyChallenges() {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting daily challenges for user {}", userId);

            List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.DAILY);

            // Include user progress
            List<UserChallenge> userChallenges = challengeService.getActiveChallenges(userId);
            Map<String, UserChallenge> userChallengeMap = userChallenges.stream()
                    .collect(Collectors.toMap(UserChallenge::getChallengeId, uc -> uc));

            List<ChallengeDto> dtos = challenges.stream()
                    .map(challenge -> {
                        UserChallenge userChallenge = userChallengeMap.get(challenge.getId());
                        return challengeMapper.toDto(challenge, userChallenge);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Daily challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting daily challenges: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve daily challenges"));
        }
    }

    /**
     * Get weekly challenges (convenience endpoint)
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getWeeklyChallenges() {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting weekly challenges for user {}", userId);

            List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.WEEKLY);

            // Include user progress
            List<UserChallenge> userChallenges = challengeService.getActiveChallenges(userId);
            Map<String, UserChallenge> userChallengeMap = userChallenges.stream()
                    .collect(Collectors.toMap(UserChallenge::getChallengeId, uc -> uc));

            List<ChallengeDto> dtos = challenges.stream()
                    .map(challenge -> {
                        UserChallenge userChallenge = userChallengeMap.get(challenge.getId());
                        return challengeMapper.toDto(challenge, userChallenge);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Weekly challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting weekly challenges: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve weekly challenges"));
        }
    }

    /**
     * Start a challenge for current user
     */
    @PostMapping("/{challengeId}/start")
    public ResponseEntity<ApiResponse<UserChallengeDto>> startChallenge(@PathVariable String challengeId) {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("User {} starting challenge {}", userId, challengeId);

            UserChallenge userChallenge = challengeService.startChallenge(userId, challengeId);
            UserChallengeDto dto = userChallengeMapper.toDto(userChallenge);

            log.info("User {} successfully started challenge {}", userId, challengeId);

            return ResponseEntity.ok(ApiResponse.success("Challenge started successfully", dto));
        } catch (IllegalArgumentException e) {
            log.warn("Challenge start failed for user {}: {}",
                    AuthenticationService.getCurrentUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error starting challenge: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to start challenge"));
        }
    }

    /**
     * Get active challenges for current user
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<UserChallengeDto>>> getActiveChallenges() {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting active challenges for user {}", userId);

            List<UserChallenge> challenges = challengeService.getActiveChallenges(userId);
            List<UserChallengeDto> dtos = challenges.stream()
                    .map(userChallengeMapper::toDto)
                    .collect(Collectors.toList());

            log.debug("Found {} active challenges for user {}", dtos.size(), userId);

            return ResponseEntity.ok(ApiResponse.success("Active challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting active challenges: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve active challenges"));
        }
    }
}