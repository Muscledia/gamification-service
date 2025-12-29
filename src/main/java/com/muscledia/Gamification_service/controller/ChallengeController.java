package com.muscledia.Gamification_service.controller;

import com.muscledia.Gamification_service.dto.request.ChallengeDto;
import com.muscledia.Gamification_service.dto.request.UserChallengeDto;
import com.muscledia.Gamification_service.dto.response.ApiResponse;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChallengeController {

    private final ChallengeService challengeService;

    /**
     * Get available challenges by type
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getAvailableChallenges(
            @RequestParam(defaultValue = "DAILY") ChallengeType type) {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("Getting {} challenges for user {}", type, userId);

            List<Challenge> challenges = challengeService.getAvailableChallenges(userId, type);
            List<ChallengeDto> dtos = challenges.stream()
                    .map(ChallengeMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(
                    type + " challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting available challenges", e);
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
            List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.DAILY);
            List<ChallengeDto> dtos = challenges.stream()
                    .map(ChallengeMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Daily challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting daily challenges", e);
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
            List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.WEEKLY);
            List<ChallengeDto> dtos = challenges.stream()
                    .map(ChallengeMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("Weekly challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting weekly challenges", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve weekly challenges"));
        }
    }

    /**
     * Start a challenge
     */
    @PostMapping("/{challengeId}/start")
    public ResponseEntity<ApiResponse<UserChallengeDto>> startChallenge(@PathVariable String challengeId) {
        try {
            Long userId = AuthenticationService.getCurrentUserId();
            log.info("User {} starting challenge {}", userId, challengeId);

            UserChallenge userChallenge = challengeService.startChallenge(userId, challengeId);
            UserChallengeDto dto = UserChallengeMapper.toDto(userChallenge);

            return ResponseEntity.ok(ApiResponse.success("Challenge started successfully", dto));
        } catch (IllegalArgumentException e) {
            log.warn("Challenge start failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error starting challenge", e);
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
                    .map(UserChallengeMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Active challenges retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting active challenges", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve active challenges"));
        }
    }
}