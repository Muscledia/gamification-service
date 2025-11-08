package com.muscledia.Gamification_service.service;


import com.muscledia.Gamification_service.model.Challenge;
import com.muscledia.Gamification_service.model.ChallengeTemplate;
import com.muscledia.Gamification_service.model.enums.ChallengeType;
import com.muscledia.Gamification_service.model.enums.DifficultyLevel;
import com.muscledia.Gamification_service.repository.ChallengeRepository;
import com.muscledia.Gamification_service.repository.ChallengeTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PURPOSE: Generate daily/weekly challenges from templates
 * RESPONSIBILITY: Create challenges based on templates and difficulty
 * COUPLING: Low - minimal dependencies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeTemplateRepository templateRepository;

    /**
     * Generate daily challenges for all difficulty levels
     */
    public List<Challenge> generateDailyChallenges() {
        log.info("Generating daily challenges");

        List<Challenge> generated = new ArrayList<>();
        Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);

        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
            Challenge challenge = createDailyChallenge(difficulty, today);
            if (challenge != null) {
                Challenge saved = challengeRepository.save(challenge);
                generated.add(saved);
            }
        }

        log.info("Generated {} daily challenges", generated.size());
        return generated;
    }

    /**
     * Generate weekly challenges
     */
    public List<Challenge> generateWeeklyChallenges() {
        log.info("Generating weekly challenges");

        List<Challenge> generated = new ArrayList<>();
        Instant weekStart = getWeekStart();

        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
            Challenge challenge = createWeeklyChallenge(difficulty, weekStart);
            if (challenge != null) {
                Challenge saved = challengeRepository.save(challenge);
                generated.add(saved);
            }
        }

        log.info("Generated {} weekly challenges", generated.size());
        return generated;
    }

    private Challenge createDailyChallenge(DifficultyLevel difficulty, Instant startDate) {
        List<ChallengeTemplate> templates = templateRepository
                .findByTypeAndDifficulty(ChallengeType.DAILY, difficulty);

        if (templates.isEmpty()) return null;

        ChallengeTemplate template = selectRandomTemplate(templates);

        return Challenge.builder()
                .name(template.getName())
                .description(template.getDescription())
                .type(ChallengeType.DAILY)
                .objectiveType(template.getObjective())
                .targetValue(template.getTargetValue(difficulty))
                .rewardPoints(template.getRewardPoints(difficulty))
                .difficultyLevel(difficulty)
                .unlockedQuestId(template.getUnlockedQuestId())
                .autoEnroll(true)
                .startDate(startDate)
                .endDate(startDate.plus(Duration.ofDays(1)))
                .createdAt(Instant.now())
                .build();
    }

    private Challenge createWeeklyChallenge(DifficultyLevel difficulty, Instant startDate) {
        // Similar to daily but with weekly duration
        List<ChallengeTemplate> templates = templateRepository
                .findByTypeAndDifficulty(ChallengeType.WEEKLY, difficulty);

        if (templates.isEmpty()) return null;

        ChallengeTemplate template = selectRandomTemplate(templates);

        return Challenge.builder()
                .name(template.getName())
                .description(template.getDescription())
                .type(ChallengeType.WEEKLY)
                .objectiveType(template.getObjective())
                .targetValue(template.getTargetValue(difficulty))
                .rewardPoints(template.getRewardPoints(difficulty))
                .difficultyLevel(difficulty)
                .unlockedQuestId(template.getUnlockedQuestId())
                .autoEnroll(false) // Weekly challenges are opt-in
                .startDate(startDate)
                .endDate(startDate.plus(Duration.ofDays(7)))
                .createdAt(Instant.now())
                .build();
    }

    private ChallengeTemplate selectRandomTemplate(List<ChallengeTemplate> templates) {
        return templates.get(new Random().nextInt(templates.size()));
    }

    private Instant getWeekStart() {
        return Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .minus(Duration.ofDays(Instant.now().atZone(ZoneOffset.UTC).getDayOfWeek().getValue() - 1));
    }

}
