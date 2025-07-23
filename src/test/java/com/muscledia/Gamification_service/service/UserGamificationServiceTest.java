package com.muscledia.Gamification_service.service;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import com.muscledia.Gamification_service.testdata.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserGamificationServiceTest {

    @Mock
    private UserGamificationProfileRepository repository;

    @InjectMocks
    private UserGamificationService userGamificationService;

    private UserGamificationProfile testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.createActiveUser();
    }

    @Test
    void shouldCreateOrGetUserProfile() {
        // Given
        Long userId = 123L;
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.save(any(UserGamificationProfile.class))).thenReturn(testUser);

        // When
        UserGamificationProfile result = userGamificationService.createOrGetUserProfile(userId);

        // Then
        assertThat(result).isNotNull();
        verify(repository).findByUserId(userId);
        verify(repository).save(any(UserGamificationProfile.class));
    }

    @Test
    void shouldGetExistingUserProfile() {
        // Given
        Long userId = testUser.getUserId();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));

        // When
        UserGamificationProfile result = userGamificationService.createOrGetUserProfile(userId);

        // Then
        assertThat(result).isEqualTo(testUser);
        verify(repository).findByUserId(userId);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldGetUserProfile() {
        // Given
        Long userId = testUser.getUserId();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));

        // When
        UserGamificationProfile result = userGamificationService.getUserProfile(userId);

        // Then
        assertThat(result).isEqualTo(testUser);
        verify(repository).findByUserId(userId);
    }

    @Test
    void shouldThrowExceptionForNonExistentUserProfile() {
        // Given
        Long userId = 999L;
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userGamificationService.getUserProfile(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User profile not found");

        verify(repository).findByUserId(userId);
    }

    @Test
    void shouldUpdateUserPoints() {
        // Given
        Long userId = testUser.getUserId();
        int pointsToAdd = 100;

        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserGamificationProfile.class))).thenReturn(testUser);

        // When
        UserGamificationProfile result = userGamificationService.updateUserPoints(userId, pointsToAdd);

        // Then
        verify(repository).findByUserId(userId);
        verify(repository).save(any(UserGamificationProfile.class));
    }

    @Test
    void shouldUpdateUserPointsAndLevel() {
        // Given
        Long userId = testUser.getUserId();
        int pointsToAdd = 2000; // Enough to trigger level up
        testUser.setPoints(800); // Starting points
        testUser.setLevel(3); // Starting level

        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserGamificationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserGamificationProfile result = userGamificationService.updateUserPoints(userId, pointsToAdd);

        // Then
        assertThat(result.getPoints()).isEqualTo(2800);
        assertThat(result.getLevel()).isGreaterThan(3); // Should level up
        verify(repository).findByUserId(userId);
        verify(repository).save(any(UserGamificationProfile.class));
    }

    @Test
    void shouldUpdateUserStreak() {
        // Given
        Long userId = testUser.getUserId();
        String streakType = "workout";
        boolean streakContinues = true;

        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserGamificationProfile.class))).thenReturn(testUser);

        // When
        UserGamificationProfile result = userGamificationService.updateUserStreak(userId, streakType, streakContinues);

        // Then
        assertThat(result).isNotNull();
        verify(repository).findByUserId(userId);
        verify(repository).save(any(UserGamificationProfile.class));
    }

    @Test
    void shouldIncrementStreakWhenContinuing() {
        // Given
        Long userId = testUser.getUserId();
        String streakType = "workout";
        boolean streakContinues = true;

        // Setup existing streak
        UserGamificationProfile.StreakData existingStreak = new UserGamificationProfile.StreakData();
        existingStreak.setCurrent(5);
        existingStreak.setLongest(10);
        existingStreak.setLastUpdate(Instant.now().minus(1, ChronoUnit.DAYS));

        Map<String, UserGamificationProfile.StreakData> streaks = new HashMap<>();
        streaks.put(streakType, existingStreak);
        testUser.setStreaks(streaks);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserGamificationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserGamificationProfile result = userGamificationService.updateUserStreak(userId, streakType, streakContinues);

        // Then
        UserGamificationProfile.StreakData updatedStreak = result.getStreaks().get(streakType);
        assertThat(updatedStreak.getCurrent()).isEqualTo(6); // Should increment
        verify(repository).save(any(UserGamificationProfile.class));
    }

    @Test
    void shouldResetStreakWhenBroken() {
        // Given
        Long userId = testUser.getUserId();
        String streakType = "workout";
        boolean streakContinues = false;

        // Setup existing streak
        UserGamificationProfile.StreakData existingStreak = new UserGamificationProfile.StreakData();
        existingStreak.setCurrent(5);
        existingStreak.setLongest(10);

        Map<String, UserGamificationProfile.StreakData> streaks = new HashMap<>();
        streaks.put(streakType, existingStreak);
        testUser.setStreaks(streaks);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserGamificationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserGamificationProfile result = userGamificationService.updateUserStreak(userId, streakType, streakContinues);

        // Then
        UserGamificationProfile.StreakData updatedStreak = result.getStreaks().get(streakType);
        assertThat(updatedStreak.getCurrent()).isEqualTo(0); // Should reset
        verify(repository).save(any(UserGamificationProfile.class));
    }

    @Test
    void shouldGetPointsLeaderboard() {
        // Given
        List<UserGamificationProfile> leaderboard = List.of(testUser);
        when(repository.findAllByOrderByPointsDesc(any(PageRequest.class))).thenReturn(leaderboard);

        // When
        List<UserGamificationProfile> result = userGamificationService.getPointsLeaderboard(10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUser);
        verify(repository).findAllByOrderByPointsDesc(PageRequest.of(0, 10));
    }

    @Test
    void shouldGetLevelLeaderboard() {
        // Given
        List<UserGamificationProfile> leaderboard = List.of(testUser);
        when(repository.findAllByOrderByLevelDesc(any(PageRequest.class))).thenReturn(leaderboard);

        // When
        List<UserGamificationProfile> result = userGamificationService.getLevelLeaderboard(10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUser);
        verify(repository).findAllByOrderByLevelDesc(PageRequest.of(0, 10));
    }

    @Test
    void shouldGetUserCurrentStreak() {
        // Given
        Long userId = testUser.getUserId();
        String streakType = "workout";

        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));

        // When
        int result = userGamificationService.getUserCurrentStreak(userId, streakType);

        // Then
        assertThat(result).isGreaterThanOrEqualTo(0);
        verify(repository).findByUserId(userId);
    }

    @Test
    void shouldGetUserLongestStreak() {
        // Given
        Long userId = testUser.getUserId();
        String streakType = "workout";

        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));

        // When
        int result = userGamificationService.getUserLongestStreak(userId, streakType);

        // Then
        assertThat(result).isGreaterThanOrEqualTo(0);
        verify(repository).findByUserId(userId);
    }

    @Test
    void shouldGetUserPointsRank() {
        // Given
        Long userId = testUser.getUserId();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.countUsersWithHigherPoints(testUser.getPoints())).thenReturn(0L);

        // When
        long result = userGamificationService.getUserPointsRank(userId);

        // Then
        assertThat(result).isEqualTo(1L); // First in list = rank 1
        verify(repository).countUsersWithHigherPoints(testUser.getPoints());
    }

    @Test
    void shouldGetUserLevelRank() {
        // Given
        Long userId = testUser.getUserId();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.countUsersWithHigherLevel(testUser.getLevel())).thenReturn(2L);

        // When
        long result = userGamificationService.getUserLevelRank(userId);

        // Then
        assertThat(result).isEqualTo(3L); // 2 users ahead = rank 3
        verify(repository).countUsersWithHigherLevel(testUser.getLevel());
    }

    @Test
    void shouldGetRecentLevelUps() {
        // Given
        int hours = 24;
        List<UserGamificationProfile> recentLevelUps = List.of(testUser);
        when(repository.findUsersWithRecentLevelUp(any(Instant.class))).thenReturn(recentLevelUps);

        // When
        List<UserGamificationProfile> result = userGamificationService.getRecentLevelUps(hours);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUser);
        verify(repository).findUsersWithRecentLevelUp(any(Instant.class));
    }

    @Test
    void shouldGetPlatformStatistics() {
        // Given
        when(repository.count()).thenReturn(1000L);
        when(repository.countByLevel(anyInt())).thenReturn(100L);
        when(repository.countByPointsGreaterThanEqual(1000)).thenReturn(300L);

        // When
        Map<String, Object> result = userGamificationService.getPlatformStatistics();

        // Then
        assertThat(result).containsKey("totalUsers");
        assertThat(result).containsKey("levelDistribution");
        assertThat(result).containsKey("pointsDistribution");
        assertThat(result.get("totalUsers")).isEqualTo(1000L);
    }

    @Test
    void shouldDeleteUserProfile() {
        // Given
        Long userId = testUser.getUserId();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));

        // When
        userGamificationService.deleteUserProfile(userId);

        // Then
        verify(repository).findByUserId(userId);
        verify(repository).delete(testUser);
    }

    @Test
    void shouldResetUserProgress() {
        // Given
        Long userId = testUser.getUserId();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserGamificationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserGamificationProfile result = userGamificationService.resetUserProgress(userId);

        // Then
        assertThat(result.getPoints()).isEqualTo(0);
        assertThat(result.getLevel()).isEqualTo(1);
        assertThat(result.getEarnedBadges()).isEmpty();
        verify(repository).findByUserId(userId);
        verify(repository).save(any(UserGamificationProfile.class));
    }
}