package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.testdata.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
class BadgeRepositoryTest {

    @Autowired
    private BadgeRepository badgeRepository;

    private Badge streakBadge;
    private Badge exerciseBadge;
    private Badge prBadge;

    @BeforeEach
    void setUp() {
        badgeRepository.deleteAll();

        // Create test badges
        streakBadge = TestDataBuilder.badge()
                .withName("Streak Master")
                .withType(BadgeType.STREAK)
                .withCriteriaType(BadgeCriteriaType.WORKOUT_STREAK)
                .withCriteriaParams(Map.of("streakType", "workout", "days", 7))
                .withPoints(200)
                .build();

        exerciseBadge = TestDataBuilder.badge()
                .withName("Exercise Expert")
                .withType(BadgeType.EXERCISE)
                .withCriteriaType(BadgeCriteriaType.WORKOUT_COUNT)
                .withCriteriaParams(Map.of("count", 50))
                .withPoints(300)
                .build();

        prBadge = TestDataBuilder.badge()
                .withName("PR Champion")
                .withType(BadgeType.PR)
                .withCriteriaType(BadgeCriteriaType.PERSONAL_RECORD)
                .withCriteriaParams(Map.of("exercise", "bench-press", "weight", 225.0))
                .withPoints(500)
                .build();

        badgeRepository.saveAll(List.of(streakBadge, exerciseBadge, prBadge));
    }

    @Test
    void shouldFindBadgeById() {
        // When
        Optional<Badge> found = badgeRepository.findById(streakBadge.getBadgeId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Streak Master");
        assertThat(found.get().getBadgeType()).isEqualTo(BadgeType.STREAK);
    }

    @Test
    void shouldFindBadgesByType() {
        // When
        List<Badge> streakBadges = badgeRepository.findByBadgeType(BadgeType.STREAK);
        List<Badge> exerciseBadges = badgeRepository.findByBadgeType(BadgeType.EXERCISE);

        // Then
        assertThat(streakBadges).hasSize(1);
        assertThat(streakBadges.get(0).getName()).isEqualTo("Streak Master");

        assertThat(exerciseBadges).hasSize(1);
        assertThat(exerciseBadges.get(0).getName()).isEqualTo("Exercise Expert");
    }

    @Test
    void shouldFindBadgesByMinimumPoints() {
        // When
        List<Badge> highValueBadges = badgeRepository.findByPointsAwardedGreaterThanEqual(300);
        List<Badge> allBadges = badgeRepository.findByPointsAwardedGreaterThanEqual(100);

        // Then
        assertThat(highValueBadges).hasSize(2);
        assertThat(highValueBadges)
                .extracting(Badge::getName)
                .containsExactlyInAnyOrder("Exercise Expert", "PR Champion");

        assertThat(allBadges).hasSize(3);
    }

    @Test
    void shouldFindBadgesByCriteriaType() {
        // When
        List<Badge> workoutCountBadges = badgeRepository.findByCriteriaType(BadgeCriteriaType.WORKOUT_COUNT);
        List<Badge> streakBadges = badgeRepository.findByCriteriaType(BadgeCriteriaType.WORKOUT_STREAK);

        // Then
        assertThat(workoutCountBadges).hasSize(1);
        assertThat(workoutCountBadges.get(0).getName()).isEqualTo("Exercise Expert");

        assertThat(streakBadges).hasSize(1);
        assertThat(streakBadges.get(0).getName()).isEqualTo("Streak Master");
    }

    @Test
    void shouldFindBadgesByCriteriaParam() {
        // When
        List<Badge> badgesWithCountParam = badgeRepository.findByCriteriaParamKey("count");
        List<Badge> badgesWithStreakTypeParam = badgeRepository.findByCriteriaParamKey("streakType");

        // Then
        assertThat(badgesWithCountParam).hasSize(1);
        assertThat(badgesWithCountParam.get(0).getName()).isEqualTo("Exercise Expert");

        assertThat(badgesWithStreakTypeParam).hasSize(1);
        assertThat(badgesWithStreakTypeParam.get(0).getName()).isEqualTo("Streak Master");
    }

    @Test
    void shouldFindBadgesByName() {
        // When
        Optional<Badge> masterBadge = badgeRepository.findByName("Streak Master");
        Optional<Badge> expertBadge = badgeRepository.findByName("Exercise Expert");

        // Then
        assertThat(masterBadge).isPresent();
        assertThat(masterBadge.get().getName()).isEqualTo("Streak Master");

        assertThat(expertBadge).isPresent();
        assertThat(expertBadge.get().getName()).isEqualTo("Exercise Expert");
    }

    @Test
    void shouldFindBadgesByTypeOrderedByPoints() {
        // When
        List<Badge> orderedBadges = badgeRepository.findByBadgeTypeOrderByPointsAwarded(BadgeType.EXERCISE);

        // Then
        assertThat(orderedBadges).hasSize(1);
        assertThat(orderedBadges.get(0).getName()).isEqualTo("Exercise Expert");
    }

    @Test
    void shouldSaveBadge() {
        // Given
        Badge newBadge = TestDataBuilder.badge()
                .withName("New Champion")
                .withType(BadgeType.CHAMPION)
                .withPoints(1000)
                .build();

        // When
        Badge saved = badgeRepository.save(newBadge);

        // Then
        assertThat(saved.getBadgeId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("New Champion");

        // Verify it can be found
        Optional<Badge> found = badgeRepository.findById(saved.getBadgeId());
        assertThat(found).isPresent();
    }

    @Test
    void shouldDeleteBadge() {
        // Given
        String badgeId = streakBadge.getBadgeId();

        // When
        badgeRepository.deleteById(badgeId);

        // Then
        Optional<Badge> found = badgeRepository.findById(badgeId);
        assertThat(found).isNotPresent();

        // Verify other badges still exist
        assertThat(badgeRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldCountBadges() {
        // When
        long count = badgeRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldFindBadgesByTypeAndMinimumPoints() {
        // When
        List<Badge> exerciseBadgesHighValue = badgeRepository.findByBadgeTypeAndPointsAwardedGreaterThanEqual(
                BadgeType.EXERCISE, 250);

        // Then
        assertThat(exerciseBadgesHighValue).hasSize(1);
        assertThat(exerciseBadgesHighValue.get(0).getName()).isEqualTo("Exercise Expert");
    }
}