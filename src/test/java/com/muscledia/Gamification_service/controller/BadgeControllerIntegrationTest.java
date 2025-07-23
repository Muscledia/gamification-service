package com.muscledia.Gamification_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muscledia.Gamification_service.config.TestSecurityConfig;
import com.muscledia.Gamification_service.model.Badge;
import com.muscledia.Gamification_service.model.UserGamificationProfile;
import com.muscledia.Gamification_service.repository.BadgeRepository;
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

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class BadgeControllerIntegrationTest {

        @Autowired
        private WebApplicationContext context;

        @Autowired
        private BadgeRepository badgeRepository;

        @Autowired
        private UserGamificationProfileRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private MockMvc mockMvc;
        private Badge testBadge;
        private UserGamificationProfile testUser;
        private String userJwtToken;
        private String adminJwtToken;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                badgeRepository.deleteAll();
                userRepository.deleteAll();

                // Create test data
                testBadge = TestDataBuilder.createStreakBadge();
                badgeRepository.save(testBadge);

                testUser = TestDataBuilder.createActiveUser();
                userRepository.save(testUser);

                // Generate JWT tokens
                userJwtToken = JwtTestUtils.generateUserToken(testUser.getUserId(), "testuser");
                adminJwtToken = JwtTestUtils.generateAdminToken(99999L, "admin");
        }

        @Test
        void shouldCreateBadgeWithAdminToken() throws Exception {
                Badge newBadge = TestDataBuilder.badge()
                                .withName("Test Creation Badge")
                                .withDescription("A badge created during testing")
                                .withPoints(300)
                                .build();

                mockMvc.perform(post("/api/badges")
                                .header("Authorization", "Bearer " + adminJwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newBadge)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.name", is("Test Creation Badge")))
                                .andExpect(jsonPath("$.data.pointsAwarded", is(300)));
        }

        @Test
        void shouldGetAllBadgesWithValidToken() throws Exception {
                mockMvc.perform(get("/api/badges")
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].name", is(testBadge.getName())));
        }

        @Test
        void shouldGetBadgesByMinimumPointsWithValidToken() throws Exception {
                int minPoints = 150;

                mockMvc.perform(get("/api/badges/min-points/{minPoints}", minPoints)
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        void shouldAwardBadgeToUserWithValidToken() throws Exception {
                mockMvc.perform(post("/api/badges/{badgeId}/award/{userId}",
                                testBadge.getBadgeId(), testUser.getUserId())
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.userId", is(testUser.getUserId().intValue())));
        }

        @Test
        void shouldCheckBadgeCriteriaWithValidToken() throws Exception {
                Map<String, Object> userStats = Map.of("workoutStreak", 10);

                mockMvc.perform(post("/api/badges/{badgeId}/check-criteria", testBadge.getBadgeId())
                                .header("Authorization", "Bearer " + userJwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userStats)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data").isBoolean());
        }

        @Test
        void shouldGetEligibleBadgesWithValidToken() throws Exception {
                Map<String, Object> userStats = Map.of("workoutStreak", 10, "totalWorkouts", 25);

                mockMvc.perform(post("/api/badges/eligible")
                                .header("Authorization", "Bearer " + userJwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userStats)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        void shouldGetUserEarnedBadgesWithValidToken() throws Exception {
                mockMvc.perform(get("/api/badges/user/{userId}", testUser.getUserId())
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        void shouldGetBadgeStatisticsWithValidToken() throws Exception {
                mockMvc.perform(get("/api/badges/statistics")
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.totalBadges").exists())
                                .andExpect(jsonPath("$.data.badgesByType").exists());
        }

        @Test
        void shouldDeleteBadgeWithAdminToken() throws Exception {
                mockMvc.perform(delete("/api/badges/{badgeId}", testBadge.getBadgeId())
                                .header("Authorization", "Bearer " + adminJwtToken))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(true)));
        }

        // ========== Security Tests ==========

        @Test
        void shouldRejectCreateBadgeWithUserToken() throws Exception {
                Badge newBadge = TestDataBuilder.badge().build();

                mockMvc.perform(post("/api/badges")
                                .header("Authorization", "Bearer " + userJwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newBadge)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectDeleteBadgeWithUserToken() throws Exception {
                mockMvc.perform(delete("/api/badges/{badgeId}", testBadge.getBadgeId())
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRejectRequestWithoutToken() throws Exception {
                mockMvc.perform(get("/api/badges"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectRequestWithInvalidToken() throws Exception {
                mockMvc.perform(get("/api/badges")
                                .header("Authorization", "Bearer invalid-token"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldHandleResourceNotFound() throws Exception {
                String nonExistentBadgeId = "non-existent-badge";

                mockMvc.perform(post("/api/badges/{badgeId}/award/{userId}",
                                nonExistentBadgeId, testUser.getUserId())
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldValidateRequestBody() throws Exception {
                Badge invalidBadge = new Badge(); // Missing required fields

                mockMvc.perform(post("/api/badges")
                                .header("Authorization", "Bearer " + adminJwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidBadge)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        void shouldHandleBusinessException() throws Exception {
                // Try to award the same badge twice to trigger business exception
                // First award
                mockMvc.perform(post("/api/badges/{badgeId}/award/{userId}",
                                testBadge.getBadgeId(), testUser.getUserId())
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isOk());

                // Second award (should fail)
                mockMvc.perform(post("/api/badges/{badgeId}/award/{userId}",
                                testBadge.getBadgeId(), testUser.getUserId())
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isConflict())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void shouldReturnConsistentApiResponseFormat() throws Exception {
                mockMvc.perform(get("/api/badges")
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").exists())
                                .andExpect(jsonPath("$.message").exists())
                                .andExpect(jsonPath("$.data").exists())
                                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldHandleInvalidPathVariable() throws Exception {
                mockMvc.perform(post("/api/badges/{badgeId}/award/{userId}",
                                "valid-badge-id", "invalid-user-id")
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldHandleMethodArgumentValidation() throws Exception {
                // Test with negative minimum points
                mockMvc.perform(get("/api/badges/min-points/{minPoints}", -1)
                                .header("Authorization", "Bearer " + userJwtToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success", is(false)));
        }
}