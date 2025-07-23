package com.muscledia.Gamification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muscledia.Gamification_service.config.TestSecurityConfig;
import com.muscledia.Gamification_service.dto.request.StreakUpdateRequest;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.UserGamificationProfileRepository;
import com.muscledia.Gamification_service.security.JwtTestUtils;
import com.muscledia.Gamification_service.testdata.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UserGamificationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserGamificationProfileRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private UserGamificationProfile testUser;
    private String userJwtToken;
    private String adminJwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        repository.deleteAll();

        // Create test user profile
        testUser = TestDataBuilder.createActiveUser();
        repository.save(testUser);

        // Generate JWT tokens
        userJwtToken = JwtTestUtils.generateUserToken(testUser.getUserId(), "testuser");
        adminJwtToken = JwtTestUtils.generateAdminToken(99999L, "admin");
    }

    @Test
    void shouldGetUserProfileWithValidToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/profile", testUser.getUserId())
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(testUser.getUserId().intValue())))
                .andExpect(jsonPath("$.data.points", is(testUser.getPoints())))
                .andExpect(jsonPath("$.data.level", is(testUser.getLevel())));
    }

    @Test
    void shouldCreateUserProfileWithValidToken() throws Exception {
        Long newUserId = 54321L;
        String newUserToken = JwtTestUtils.generateUserToken(newUserId, "newuser");

        mockMvc.perform(post("/api/users/{userId}/profile", newUserId)
                .header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(newUserId.intValue())))
                .andExpect(jsonPath("$.data.points", is(0)))
                .andExpect(jsonPath("$.data.level", is(1)));
    }

    @Test
    void shouldUpdateUserPointsWithValidToken() throws Exception {
        int pointsToAdd = 500;

        mockMvc.perform(put("/api/users/{userId}/points", testUser.getUserId())
                .header("Authorization", "Bearer " + userJwtToken)
                .param("points", String.valueOf(pointsToAdd)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(testUser.getUserId().intValue())));
    }

    @Test
    void shouldUpdateUserStreakWithValidToken() throws Exception {
        StreakUpdateRequest request = new StreakUpdateRequest();
        request.setUserId(testUser.getUserId());
        request.setStreakType("workout");
        request.setStreakContinues(true);

        mockMvc.perform(put("/api/users/{userId}/streaks", testUser.getUserId())
                .header("Authorization", "Bearer " + userJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void shouldGetPointsLeaderboardWithValidToken() throws Exception {
        mockMvc.perform(get("/api/users/leaderboards/points")
                .header("Authorization", "Bearer " + userJwtToken)
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalUsers").exists())
                .andExpect(jsonPath("$.data.users").isArray());
    }

    @Test
    void shouldGetLevelsLeaderboardWithValidToken() throws Exception {
        mockMvc.perform(get("/api/users/leaderboards/levels")
                .header("Authorization", "Bearer " + userJwtToken)
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalUsers").exists())
                .andExpect(jsonPath("$.data.users").isArray());
    }

    @Test
    void shouldGetUserCurrentStreakWithValidToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/streaks/{streakType}/current",
                testUser.getUserId(), "workout")
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void shouldGetUserLongestStreakWithValidToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/streaks/{streakType}/longest",
                testUser.getUserId(), "workout")
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void shouldGetUserPointsRankWithValidToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/rank/points", testUser.getUserId())
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void shouldGetUserLevelRankWithValidToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/rank/level", testUser.getUserId())
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void shouldAllowAdminToAccessPlatformStatistics() throws Exception {
        mockMvc.perform(get("/api/users/analytics/platform-stats")
                .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalUsers").exists())
                .andExpect(jsonPath("$.data.highLevelUsers").exists())
                .andExpect(jsonPath("$.data.activeUsers").exists());
    }

    @Test
    void shouldAllowAdminToResetUserProgress() throws Exception {
        mockMvc.perform(put("/api/users/{userId}/reset", testUser.getUserId())
                .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.points", is(0)))
                .andExpect(jsonPath("$.data.level", is(1)));
    }

    @Test
    void shouldAllowAdminToDeleteUserProfile() throws Exception {
        mockMvc.perform(delete("/api/users/{userId}/profile", testUser.getUserId())
                .header("Authorization", "Bearer " + adminJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)));
    }

    // ========== Security Tests ==========

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/profile", testUser.getUserId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/profile", testUser.getUserId())
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        String expiredToken = JwtTestUtils.generateExpiredToken(testUser.getUserId(), "testuser");

        mockMvc.perform(get("/api/users/{userId}/profile", testUser.getUserId())
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserAccessingOtherUserProfile() throws Exception {
        Long otherUserId = 99999L;

        mockMvc.perform(get("/api/users/{userId}/profile", otherUserId)
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUserAccessingAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/analytics/platform-stats")
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUserResettingOtherUserProgress() throws Exception {
        Long otherUserId = 99999L;

        mockMvc.perform(put("/api/users/{userId}/reset", otherUserId)
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUserDeletingOtherUserProfile() throws Exception {
        Long otherUserId = 99999L;

        mockMvc.perform(delete("/api/users/{userId}/profile", otherUserId)
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldValidateRequestBody() throws Exception {
        StreakUpdateRequest invalidRequest = new StreakUpdateRequest();
        // Missing required fields

        mockMvc.perform(put("/api/users/{userId}/streaks", testUser.getUserId())
                .header("Authorization", "Bearer " + userJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void shouldHandleResourceNotFound() throws Exception {
        Long nonExistentUserId = 99999L;
        String tokenForNonExistentUser = JwtTestUtils.generateUserToken(nonExistentUserId, "nonexistent");

        mockMvc.perform(get("/api/users/{userId}/profile", nonExistentUserId)
                .header("Authorization", "Bearer " + tokenForNonExistentUser))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldHandleInvalidPathVariable() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/profile", "invalid-user-id")
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConsistentApiResponseFormat() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/profile", testUser.getUserId())
                .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}