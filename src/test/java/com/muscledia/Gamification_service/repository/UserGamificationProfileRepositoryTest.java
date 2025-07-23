package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.testdata.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
class UserGamificationProfileRepositoryTest {

    @Autowired
    private UserGamificationProfileRepository repository;

    private UserGamificationProfile user1;
    private UserGamificationProfile user2;
    private UserGamificationProfile user3;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Create test users with different levels and points
        user1 = TestDataBuilder.userProfile()
                .withUserId(1001L)
                .withPoints(1500)
                .withLevel(5)
                .withLastLevelUpDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        user2 = TestDataBuilder.userProfile()
                .withUserId(1002L)
                .withPoints(2500)
                .withLevel(8)
                .withLastLevelUpDate(Instant.now().minus(3, ChronoUnit.DAYS))
                .build();

        user3 = TestDataBuilder.userProfile()
                .withUserId(1003L)
                .withPoints(800)
                .withLevel(3)
                .withLastLevelUpDate(Instant.now().minus(7, ChronoUnit.DAYS))
                .build();

        // Add streak data to users
        addStreakData(user1, "workout", 10, 15);
        addStreakData(user2, "workout", 7, 12);
        addStreakData(user3, "workout", 3, 8);

        repository.saveAll(List.of(user1, user2, user3));
    }

    private void addStreakData(UserGamificationProfile user, String streakType, int current, int longest) {
        Map<String, UserGamificationProfile.StreakData> streaks = new HashMap<>(user.getStreaks());
        UserGamificationProfile.StreakData streakData = new UserGamificationProfile.StreakData();
        streakData.setCurrent(current);
        streakData.setLongest(longest);
        streakData.setLastUpdate(Instant.now());
        streaks.put(streakType, streakData);
        user.setStreaks(streaks);
    }

    @Test
    void shouldFindUserById() {
        // When
        Optional<UserGamificationProfile> found = repository.findById(1001L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1001L);
        assertThat(found.get().getPoints()).isEqualTo(1500);
        assertThat(found.get().getLevel()).isEqualTo(5);
    }

    @Test
    void shouldFindUserByUserId() {
        // When
        Optional<UserGamificationProfile> found = repository.findByUserId(1001L);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1001L);
        assertThat(found.get().getPoints()).isEqualTo(1500);
        assertThat(found.get().getLevel()).isEqualTo(5);
    }

    @Test
    void shouldFindAllOrderedByPointsDesc() {
        // When
        List<UserGamificationProfile> users = repository.findAllByOrderByPointsDesc(PageRequest.of(0, 10));

        // Then
        assertThat(users).hasSize(3);
        assertThat(users)
                .extracting(UserGamificationProfile::getUserId)
                .containsExactly(1002L, 1001L, 1003L);
        assertThat(users)
                .extracting(UserGamificationProfile::getPoints)
                .containsExactly(2500, 1500, 800);
    }

    @Test
    void shouldFindAllOrderedByLevelDesc() {
        // When
        List<UserGamificationProfile> users = repository.findAllByOrderByLevelDesc(PageRequest.of(0, 10));

        // Then
        assertThat(users).hasSize(3);
        assertThat(users)
                .extracting(UserGamificationProfile::getLevel)
                .containsExactly(8, 5, 3);
    }

    @Test
    void shouldFindUsersByMinimumLevel() {
        // When
        List<UserGamificationProfile> highLevelUsers = repository.findByLevelGreaterThanEqual(5);

        // Then
        assertThat(highLevelUsers).hasSize(2);
        assertThat(highLevelUsers)
                .extracting(UserGamificationProfile::getUserId)
                .containsExactlyInAnyOrder(1001L, 1002L);
    }

    @Test
    void shouldFindUsersByMinimumPoints() {
        // When
        List<UserGamificationProfile> highPointUsers = repository.findByPointsGreaterThanEqual(1500);

        // Then
        assertThat(highPointUsers).hasSize(2);
        assertThat(highPointUsers)
                .extracting(UserGamificationProfile::getUserId)
                .containsExactlyInAnyOrder(1001L, 1002L);
    }

    @Test
    void shouldFindRecentLevelUps() {
        // Given
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);

        // When
        List<UserGamificationProfile> recentLevelUps = repository.findUsersWithRecentLevelUp(twoDaysAgo);

        // Then
        assertThat(recentLevelUps).hasSize(1);
        assertThat(recentLevelUps.get(0).getUserId()).isEqualTo(1001L);
    }

    @Test
    void shouldCountTotalUsers() {
        // When
        long totalUsers = repository.count();

        // Then
        assertThat(totalUsers).isEqualTo(3);
    }

    @Test
    void shouldCountUsersByMinimumPoints() {
        // When
        long highPointUsers = repository.countByPointsGreaterThanEqual(1500);

        // Then
        assertThat(highPointUsers).isEqualTo(2);
    }

    @Test
    void shouldCountUsersByLevel() {
        // When
        long level5Users = repository.countByLevel(5);

        // Then
        assertThat(level5Users).isEqualTo(1);
    }

    @Test
    void shouldFindUsersWithActiveStreak() {
        // When
        List<UserGamificationProfile> activeStreakUsers = repository.findUsersWithActiveStreak("workout");

        // Then
        assertThat(activeStreakUsers).hasSize(3);
    }

    @Test
    void shouldFindTopUsersByStreak() {
        // When
        List<UserGamificationProfile> topStreakUsers = repository.findTopUsersByStreak("workout", PageRequest.of(0, 2));

        // Then
        assertThat(topStreakUsers).hasSize(2);
    }

    @Test
    void shouldSaveAndUpdateUserProfile() {
        // Given
        UserGamificationProfile newUser = TestDataBuilder.userProfile()
                .withUserId(1004L)
                .withPoints(500)
                .withLevel(2)
                .build();

        // When
        UserGamificationProfile saved = repository.save(newUser);

        // Then
        assertThat(saved.getUserId()).isEqualTo(1004L);

        // Update and save again
        saved.setPoints(1000);
        saved.setLevel(4);
        UserGamificationProfile updated = repository.save(saved);

        assertThat(updated.getPoints()).isEqualTo(1000);
        assertThat(updated.getLevel()).isEqualTo(4);
    }

    @Test
    void shouldDeleteUserProfile() {
        // Given
        Long userId = 1001L;

        // When
        repository.deleteById(userId);

        // Then
        Optional<UserGamificationProfile> found = repository.findById(userId);
        assertThat(found).isNotPresent();
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void shouldExistsByUserId() {
        // When & Then
        assertThat(repository.existsByUserId(1001L)).isTrue();
        assertThat(repository.existsByUserId(9999L)).isFalse();
    }

    @Test
    void shouldCountUsersWithHigherPoints() {
        // When
        long usersWithHigherPoints = repository.countUsersWithHigherPoints(1500);

        // Then
        assertThat(usersWithHigherPoints).isEqualTo(1); // Only user2 has more than 1500 points
    }

    @Test
    void shouldCountUsersWithHigherLevel() {
        // When
        long usersWithHigherLevel = repository.countUsersWithHigherLevel(5);

        // Then
        assertThat(usersWithHigherLevel).isEqualTo(1); // Only user2 has level higher than 5
    }
}