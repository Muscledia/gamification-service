package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.repository.BadgeRepository;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import com.muscledia.Gamification_service.testdata.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserGamificationProfileRepository userProfileRepository;

    @InjectMocks
    private BadgeService badgeService;

    private Badge testBadge;
    private UserGamificationProfile testUser;

    @BeforeEach
    void setUp() {
        testBadge = TestDataBuilder.createStreakBadge();
        testUser = TestDataBuilder.createActiveUser();
    }

    @Test
    void shouldCreateBadge() {
        // Given
        when(badgeRepository.existsByName(testBadge.getName())).thenReturn(false);
        when(badgeRepository.save(any(Badge.class))).thenReturn(testBadge);

        // When
        Badge result = badgeService.createBadge(testBadge);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Streak Master");
        verify(badgeRepository).existsByName(testBadge.getName());
        verify(badgeRepository).save(testBadge);
    }

    @Test
    void shouldGetAllBadges() {
        // Given
        List<Badge> badges = List.of(testBadge);
        when(badgeRepository.findAll()).thenReturn(badges);

        // When
        List<Badge> result = badgeService.getAllBadges(null, null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Streak Master");
        verify(badgeRepository).findAll();
    }

    @Test
    void shouldGetBadgesByType() {
        // Given
        List<Badge> streakBadges = List.of(testBadge);
        when(badgeRepository.findByBadgeType(BadgeType.STREAK)).thenReturn(streakBadges);

        // When
        List<Badge> result = badgeService.getAllBadges(BadgeType.STREAK, null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBadgeType()).isEqualTo(BadgeType.STREAK);
        verify(badgeRepository).findByBadgeType(BadgeType.STREAK);
    }

    @Test
    void shouldGetBadgesByMinimumPoints() {
        // Given
        List<Badge> highValueBadges = List.of(testBadge);
        when(badgeRepository.findByPointsAwardedGreaterThanEqual(150)).thenReturn(highValueBadges);

        // When
        List<Badge> result = badgeService.getBadgesByMinPoints(150);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPointsAwarded()).isGreaterThanOrEqualTo(150);
        verify(badgeRepository).findByPointsAwardedGreaterThanEqual(150);
    }

    @Test
    void shouldAwardBadgeToUser() {
        // Given
        String badgeId = testBadge.getBadgeId();
        Long userId = testUser.getUserId();

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(testBadge));
        when(userProfileRepository.save(any(UserGamificationProfile.class))).thenReturn(testUser);

        // When
        UserGamificationProfile result = badgeService.awardBadge(userId, badgeId);

        // Then
        assertThat(result).isNotNull();
        verify(userProfileRepository).findByUserId(userId);
        verify(badgeRepository).findById(badgeId);
        verify(userProfileRepository).save(any(UserGamificationProfile.class));
    }

    @Test
    void shouldThrowExceptionWhenAwardingNonExistentBadge() {
        // Given
        String badgeId = "non-existent";
        Long userId = 123L;

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(badgeRepository.findById(badgeId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> badgeService.awardBadge(userId, badgeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge not found");

        verify(userProfileRepository).findByUserId(userId);
        verify(badgeRepository).findById(badgeId);
    }

    @Test
    void shouldCheckBadgeCriteriaForWorkoutCount() {
        // Given
        Badge workoutBadge = TestDataBuilder.badge()
                .withCriteriaType(BadgeCriteriaType.WORKOUT_COUNT)
                .withCriteriaParams(Map.of("targetValue", 10))
                .build();

        Map<String, Object> userStats = Map.of("workoutCount", 15);

        when(badgeRepository.findById(anyString())).thenReturn(Optional.of(workoutBadge));

        // When
        boolean result = badgeService.checkBadgeCriteria(123L, workoutBadge.getBadgeId(), userStats);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldFailBadgeCriteriaForInsufficientWorkouts() {
        // Given
        Badge workoutBadge = TestDataBuilder.badge()
                .withCriteriaType(BadgeCriteriaType.WORKOUT_COUNT)
                .withCriteriaParams(Map.of("targetValue", 10))
                .build();

        Map<String, Object> userStats = Map.of("workoutCount", 5);

        when(badgeRepository.findById(anyString())).thenReturn(Optional.of(workoutBadge));

        // When
        boolean result = badgeService.checkBadgeCriteria(123L, workoutBadge.getBadgeId(), userStats);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldCheckBadgeCriteriaForWorkoutStreak() {
        // Given
        Badge streakBadge = TestDataBuilder.badge()
                .withCriteriaType(BadgeCriteriaType.WORKOUT_STREAK)
                .withCriteriaParams(Map.of("targetValue", 7))
                .build();

        Map<String, Object> userStats = Map.of("currentWorkoutStreak", 10);

        when(badgeRepository.findById(anyString())).thenReturn(Optional.of(streakBadge));

        // When
        boolean result = badgeService.checkBadgeCriteria(123L, streakBadge.getBadgeId(), userStats);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldGetEligibleBadgesForUser() {
        // Given
        Long userId = testUser.getUserId();
        Badge eligibleBadge = TestDataBuilder.badge()
                .withCriteriaType(BadgeCriteriaType.WORKOUT_STREAK)
                .withCriteriaParams(Map.of("targetValue", 7))
                .build();
        List<Badge> allBadges = List.of(eligibleBadge);
        Map<String, Object> userStats = Map.of("currentWorkoutStreak", 10);

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(badgeRepository.findAll()).thenReturn(allBadges);

        // When
        List<Badge> result = badgeService.getEligibleBadges(userId, userStats);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCriteriaType()).isEqualTo(BadgeCriteriaType.WORKOUT_STREAK);
        verify(userProfileRepository).findByUserId(userId);
        verify(badgeRepository).findAll();
    }

    @Test
    void shouldGetUserEarnedBadges() {
        // Given
        Long userId = testUser.getUserId();
        List<String> badgeIds = testUser.getEarnedBadges().stream()
                .map(ub -> ub.getBadgeId())
                .toList();

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(badgeRepository.findAllById(badgeIds)).thenReturn(List.of(testBadge));

        // When
        List<Badge> result = badgeService.getUserBadges(userId);

        // Then
        assertThat(result).hasSize(1);
        verify(userProfileRepository).findByUserId(userId);
        verify(badgeRepository).findAllById(badgeIds);
    }

    @Test
    void shouldGetBadgeStatistics() {
        // Given
        when(badgeRepository.count()).thenReturn(50L);
        when(badgeRepository.findByBadgeType(BadgeType.STREAK)).thenReturn(List.of(testBadge));
        when(badgeRepository.findByBadgeType(BadgeType.EXERCISE)).thenReturn(List.of());
        when(badgeRepository.findByBadgeType(BadgeType.PR)).thenReturn(List.of());
        when(badgeRepository.findByBadgeType(BadgeType.SEASONAL)).thenReturn(List.of());
        when(badgeRepository.findByBadgeType(BadgeType.CHAMPION)).thenReturn(List.of());

        // When
        Map<String, Object> result = badgeService.getBadgeStatistics();

        // Then
        assertThat(result).containsKey("totalBadges");
        assertThat(result).containsKey("badgesByType");
        assertThat(result.get("totalBadges")).isEqualTo(50L);

        @SuppressWarnings("unchecked")
        Map<BadgeType, Long> badgesByType = (Map<BadgeType, Long>) result.get("badgesByType");
        assertThat(badgesByType.get(BadgeType.STREAK)).isEqualTo(1L);
        assertThat(badgesByType.get(BadgeType.EXERCISE)).isEqualTo(0L);
    }

    @Test
    void shouldDeleteBadge() {
        // Given
        String badgeId = testBadge.getBadgeId();
        when(badgeRepository.existsById(badgeId)).thenReturn(true);

        // When
        badgeService.deleteBadge(badgeId);

        // Then
        verify(badgeRepository).existsById(badgeId);
        verify(badgeRepository).deleteById(badgeId);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentBadge() {
        // Given
        String badgeId = "non-existent";
        when(badgeRepository.existsById(badgeId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> badgeService.deleteBadge(badgeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Badge not found");

        verify(badgeRepository).existsById(badgeId);
        verify(badgeRepository, never()).deleteById(anyString());
    }

    @Test
    void shouldHandleUnsupportedCriteriaType() {
        // Given
        Badge unknownCriteriaBadge = TestDataBuilder.badge()
                .withCriteriaType(BadgeCriteriaType.CALORIE_GOALS_MET)
                .build();

        Map<String, Object> userStats = Map.of("calories", 2000);

        when(badgeRepository.findById(anyString())).thenReturn(Optional.of(unknownCriteriaBadge));

        // When
        boolean result = badgeService.checkBadgeCriteria(123L, unknownCriteriaBadge.getBadgeId(), userStats);

        // Then
        assertThat(result).isFalse(); // Should default to false for unsupported criteria
    }
}