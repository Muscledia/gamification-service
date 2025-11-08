package com.muscledia.Gamification_service.controller;


import com.muscledia.Gamification_service.dto.request.ChallengeDto;
import com.muscledia.Gamification_service.dto.request.UserChallengeDto;
import com.muscledia.Gamification_service.mapper.ChallengeMapper;
import com.muscledia.Gamification_service.mapper.UserChallengeMapper;
import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.UserChallenge;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.security.UserPrincipal;
import com.muscledia.Gamification_service.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<List<ChallengeDto>> getDailyChallenges(Authentication auth) {
        Long userId = extractUserId(auth);
        List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.DAILY);
        List<ChallengeDto> dtos = challenges.stream()
                .map(ChallengeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<ChallengeDto>> getWeeklyChallenges(Authentication auth) {
        Long userId = extractUserId(auth);
        List<Challenge> challenges = challengeService.getAvailableChallenges(userId, ChallengeType.WEEKLY);
        List<ChallengeDto> dtos = challenges.stream()
                .map(ChallengeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{challengeId}/start")
    public ResponseEntity<UserChallengeDto> startChallenge(
            @PathVariable String challengeId,
            Authentication auth) {
        Long userId = extractUserId(auth);
        UserChallenge userChallenge = challengeService.startChallenge(userId, challengeId);
        UserChallengeDto dto = UserChallengeMapper.toDto(userChallenge);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserChallengeDto>> getActiveChallenges(Authentication auth) {
        Long userId = extractUserId(auth);
        List<UserChallenge> challenges = challengeService.getActiveChallenges(userId);
        List<UserChallengeDto> dtos = challenges.stream()
                .map(UserChallengeMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private Long extractUserId(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getUserId();
    }
}
