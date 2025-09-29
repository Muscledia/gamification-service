# Muscledia Gamification Service - API Endpoints

## Overview

This document provides a brief overview of all REST API endpoints available in the Muscledia Gamification Service.

**Base URL:** `http://localhost:8083`  
**Authentication:** Bearer JWT Token (from muscledia-user-service)  
**Content-Type:** `application/json`

---

## 🏆 Badge Management (`/api/badges`)

| Method | Endpoint                               | Description                        | Return Type                            |
| ------ | -------------------------------------- | ---------------------------------- | -------------------------------------- |
| POST   | `/api/badges`                          | Create a new badge                 | `ApiResponse<Badge>`                   |
| GET    | `/api/badges`                          | Get all badges (with filters)      | `ApiResponse<List<Badge>>`             |
| GET    | `/api/badges/min-points/{minPoints}`   | Get badges by minimum points       | `ApiResponse<List<Badge>>`             |
| POST   | `/api/badges/{badgeId}/award/{userId}` | Award badge to user                | `ApiResponse<UserGamificationProfile>` |
| POST   | `/api/badges/{badgeId}/check-criteria` | Check if user meets badge criteria | `ApiResponse<Boolean>`                 |
| POST   | `/api/badges/eligible`                 | Get eligible badges for user       | `ApiResponse<List<Badge>>`             |
| GET    | `/api/badges/user/{userId}`            | Get user's earned badges           | `ApiResponse<List<Badge>>`             |
| GET    | `/api/badges/statistics`               | Get badge statistics               | `ApiResponse<Map<String, Object>>`     |
| DELETE | `/api/badges/{badgeId}`                | Delete a badge                     | `ApiResponse<Void>`                    |

---

## 🎯 Quest Management (`/api/quests`)

| Method | Endpoint                                  | Description                       | Return Type                            |
| ------ | ----------------------------------------- | --------------------------------- | -------------------------------------- |
| POST   | `/api/quests`                             | Create a new quest                | `ApiResponse<Quest>`                   |
| GET    | `/api/quests/user/{userId}/active`        | Get active quests for user        | `ApiResponse<List<Quest>>`             |
| GET    | `/api/quests`                             | Get all quests (with filters)     | `ApiResponse<List<Quest>>`             |
| POST   | `/api/quests/{questId}/start/{userId}`    | Start quest for user              | `ApiResponse<UserGamificationProfile>` |
| PUT    | `/api/quests/{questId}/progress`          | Update quest progress             | `ApiResponse<UserGamificationProfile>` |
| POST   | `/api/quests/{questId}/complete/{userId}` | Complete quest for user           | `ApiResponse<UserGamificationProfile>` |
| GET    | `/api/quests/user/{userId}/progress`      | Get user quest progress           | `ApiResponse<List<UserQuestProgress>>` |
| GET    | `/api/quests/type-difficulty`             | Get quests by type and difficulty | `ApiResponse<List<Quest>>`             |
| GET    | `/api/quests/upcoming`                    | Get upcoming quests               | `ApiResponse<List<Quest>>`             |
| GET    | `/api/quests/expired`                     | Get expired quests                | `ApiResponse<List<Quest>>`             |
| GET    | `/api/quests/statistics`                  | Get quest statistics              | `ApiResponse<Map<String, Object>>`     |
| DELETE | `/api/quests/{questId}`                   | Delete a quest                    | `ApiResponse<Void>`                    |
| POST   | `/api/quests/process-expired`             | Process expired quests (Admin)    | `ApiResponse<Void>`                    |
| POST   | `/api/quests/generate-scheduled`          | Generate scheduled quests (Admin) | `ApiResponse<List<Quest>>`             |

---

## 👑 Champion Management (`/api/champions`)

| Method | Endpoint                                      | Description                                | Return Type                            |
| ------ | --------------------------------------------- | ------------------------------------------ | -------------------------------------- |
| POST   | `/api/champions`                              | Create a new champion                      | `ApiResponse<Champion>`                |
| GET    | `/api/champions`                              | Get all champions (with filters)           | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/difficulty/{difficulty}`      | Get champions by specific difficulty       | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/difficulty-range`             | Get champions by difficulty range          | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/exercise/{exerciseId}`        | Get champions for specific exercise        | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/muscle-group/{muscleGroupId}` | Get champions for muscle group             | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/general`                      | Get general champions                      | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/exercise-specific`            | Get exercise-specific champions            | `ApiResponse<List<Champion>>`          |
| POST   | `/api/champions/{championId}/check-criteria`  | Check if user meets champion criteria      | `ApiResponse<Boolean>`                 |
| POST   | `/api/champions/eligible`                     | Get eligible champions for user            | `ApiResponse<List<Champion>>`          |
| POST   | `/api/champions/{championId}/award/{userId}`  | Award champion to user                     | `ApiResponse<UserGamificationProfile>` |
| GET    | `/api/champions/statistics`                   | Get champion statistics                    | `ApiResponse<Map<String, Object>>`     |
| GET    | `/api/champions/difficulty/ascending`         | Get champions ordered by difficulty (asc)  | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/difficulty/descending`        | Get champions ordered by difficulty (desc) | `ApiResponse<List<Champion>>`          |
| GET    | `/api/champions/recent`                       | Get recently created champions             | `ApiResponse<List<Champion>>`          |
| PUT    | `/api/champions/{championId}`                 | Update a champion                          | `ApiResponse<Champion>`                |
| DELETE | `/api/champions/{championId}`                 | Delete a champion                          | `ApiResponse<Void>`                    |

---

## 👤 User Gamification Management (`/api/users`)

### User Profile Management

| Method | Endpoint                           | Description                   | Return Type                            |
| ------ | ---------------------------------- | ----------------------------- | -------------------------------------- |
| POST   | `/api/users/{userId}/profile`      | Create or get user profile    | `ApiResponse<UserGamificationProfile>` |
| GET    | `/api/users/{userId}/profile`      | Get user profile              | `ApiResponse<UserGamificationProfile>` |
| PUT    | `/api/users/{userId}/points`       | Update user points            | `ApiResponse<UserGamificationProfile>` |
| PUT    | `/api/users/{userId}/streaks`      | Update user streak            | `ApiResponse<UserGamificationProfile>` |
| GET    | `/api/users/{userId}/achievements` | Get user achievements summary | `ApiResponse<Map<String, Object>>`     |
| PUT    | `/api/users/{userId}/reset`        | Reset user progress (Admin)   | `ApiResponse<UserGamificationProfile>` |
| DELETE | `/api/users/{userId}/profile`      | Delete user profile (Admin)   | `ApiResponse<Void>`                    |

### User Statistics

| Method | Endpoint                                           | Description             | Return Type            |
| ------ | -------------------------------------------------- | ----------------------- | ---------------------- |
| GET    | `/api/users/{userId}/streaks/{streakType}/current` | Get user current streak | `ApiResponse<Integer>` |
| GET    | `/api/users/{userId}/streaks/{streakType}/longest` | Get user longest streak | `ApiResponse<Integer>` |
| GET    | `/api/users/{userId}/rank/points`                  | Get user points rank    | `ApiResponse<Long>`    |
| GET    | `/api/users/{userId}/rank/level`                   | Get user level rank     | `ApiResponse<Long>`    |

### Leaderboards

| Method | Endpoint                                               | Description                    | Return Type                        |
| ------ | ------------------------------------------------------ | ------------------------------ | ---------------------------------- |
| GET    | `/api/users/leaderboards/points`                       | Get points leaderboard         | `ApiResponse<LeaderboardResponse>` |
| GET    | `/api/users/leaderboards/levels`                       | Get level leaderboard          | `ApiResponse<LeaderboardResponse>` |
| GET    | `/api/users/leaderboards/streaks/{streakType}`         | Get streak leaderboard         | `ApiResponse<LeaderboardResponse>` |
| GET    | `/api/users/leaderboards/streaks/{streakType}/longest` | Get longest streak leaderboard | `ApiResponse<LeaderboardResponse>` |

### Analytics

| Method | Endpoint                                            | Description                     | Return Type                                  |
| ------ | --------------------------------------------------- | ------------------------------- | -------------------------------------------- |
| GET    | `/api/users/analytics/recent-levelups`              | Get recent level ups            | `ApiResponse<List<UserGamificationProfile>>` |
| GET    | `/api/users/analytics/active-streaks/{streakType}`  | Get users with active streak    | `ApiResponse<List<UserGamificationProfile>>` |
| GET    | `/api/users/analytics/minimum-streaks/{streakType}` | Get users with minimum streak   | `ApiResponse<List<UserGamificationProfile>>` |
| GET    | `/api/users/analytics/platform-stats`               | Get platform statistics (Admin) | `ApiResponse<Map<String, Object>>`           |

---

## 🔐 Authentication & Access Control

### Access Levels:

- **🌐 Public**: Swagger UI, API docs, health checks
- **🔒 Authenticated**: All API endpoints require valid JWT token
- **👤 User-Restricted**: Users can only access their own resources
- **👑 Admin-Only**: Marked with (Admin) - requires ADMIN role

### JWT Token Format:

```json
{
  "sub": "username",
  "userId": 123,
  "username": "user",
  "email": "user@example.com",
  "roles": ["USER"],
  "iss": "muscledia-user-service",
  "iat": 1234567890,
  "exp": 1234567890
}
```

### Common Response Format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    /* response data */
  },
  "timestamp": "2024-01-01T10:00:00Z"
}
```

---

## 📚 Documentation

- **Swagger UI**: `/swagger-ui.html`
- **OpenAPI Docs**: `/v3/api-docs`
- **Health Check**: `/actuator/health`

## 🔧 Configuration

- **Port**: 8083
- **Database**: MongoDB
- **Authentication**: JWT Bearer tokens
- **CORS**: Enabled for frontend integration
