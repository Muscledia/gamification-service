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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PURPOSE: Expose challenge operations via REST API
 * RESPONSIBILITY: Handle HTTP requests and responses
 * COUPLING: Low - depends only on service interfaces
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Validated
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getDailyChallenges() {
        Long userId = AuthenticationService.getCurrentUserId();
        List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.DAILY);
        List<ChallengeDto> dtos = challenges.stream()
                .map(ChallengeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Daily challenges retrieved successfully", dtos));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getWeeklyChallenges() {
        Long userId = AuthenticationService.getCurrentUserId();
        List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.WEEKLY);
        List<ChallengeDto> dtos = challenges.stream()
                .map(ChallengeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Weekly challenges retrieved successfully", dtos));
    }

    @PostMapping("/{challengeId}/start")
    public ResponseEntity<ApiResponse<UserChallengeDto>> startChallenge(@PathVariable String challengeId) {
        Long userId = AuthenticationService.getCurrentUserId();
        UserChallenge userChallenge = challengeService.startChallenge(userId, challengeId);
        UserChallengeDto dto = UserChallengeMapper.toDto(userChallenge);
        return ResponseEntity.ok(ApiResponse.success("Challenge started successfully", dto));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<UserChallengeDto>>> getActiveChallenges() {
        Long userId = AuthenticationService.getCurrentUserId();
        List<UserChallenge> challenges = challengeService.getActiveChallenges(userId);
        List<UserChallengeDto> dtos = challenges.stream()
                .map(UserChallengeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Active challenges retrieved successfully", dtos));
    }
}
