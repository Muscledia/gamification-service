# Gamification Service

This microservice is a core component of the Muscledia ecosystem, dedicated to enhancing user engagement and motivation through gamification. It tracks user progress, awards points, manages levels, and grants badges based on activity events from other services, primarily the `workout-service`.

## Table of Contents

1.  [Overview](#1-overview)
2.  [Responsibilities](#2-responsibilities)
3.  [Technology Stack](#3-technology-stack)
4.  [Core Entities / Data Model](#4-core-entities--data-model)
5.  [Event-Driven Architecture](#5-event-driven-architecture)
    - [Events Consumed](#events-consumed)
    - [Events Published](#events-published-future-consideration)
6.  [API Endpoints](#6-api-endpoints)
7.  [Getting Started](#7-getting-started)
    - [Prerequisites](#prerequisites)
    - [Running Locally](#running-locally)
    - [Configuration](#configuration)
8.  [Development Roadmap](#8-development-roadmap)
9.  [Future Enhancements](#9-future-enhancements)

---

## 1. Overview

The `gamification-service` operates on an event-driven model, ensuring loose coupling with other services. It consumes relevant user activity events, processes them against defined gamification rules, and updates the user's gamified profile in real-time. This service provides the necessary APIs for the frontend to display a user's achievements and global leaderboards.

## 2. Responsibilities

- **Event Subscription:** Listen for and consume specific user activity events (e.g., `WorkoutCompletedEvent`, `ExerciseLoggedEvent`) from **Kafka topics**.
- **Gamification Rule Processing:** Evaluate incoming events against a predefined set of rules to determine eligibility for points, level progression, and badge awards.
- **User State Management:** Persist and update users' current points, levels, earned badges, streaks, and other progress metrics within its dedicated database.
- **Achievement Query APIs:** Expose robust RESTful endpoints for querying a user's comprehensive gamification profile, specific earned badges, and various leaderboard data.
- **Achievement Event Publishing (Future):** Potentially publish events (e.g., `AchievementEarnedEvent`) to notify other services (like a `notification-service`) about significant user achievements via **Kafka topics**.

## 3. Technology Stack

- **Language:** Java
- **Framework:** Spring Boot (Leveraging Spring WebFlux for reactive and non-blocking operations)
- **Database:** MongoDB (Chosen for its flexible document model, ideal for varied gamification data, and available free tiers like MongoDB Atlas M0).
- **Messaging:** **Apache Kafka** for robust, high-throughput, and durable asynchronous event communication.
  - **Libraries:** **Spring Kafka**.
- **Build Tool:** Maven (or Gradle, ensuring consistency with other Muscledia microservices).
- **Dependencies:** Spring Boot Starters for Reactive Web (WebFlux), MongoDB Reactive, **Kafka**, and Lombok for boilerplate reduction.

## 4. Core Entities / Data Model

The `gamification-service` will primarily manage the following entities within MongoDB. This leverages MongoDB's document-oriented nature by using collections for static definitions and embedding user-specific data within the `UserGamificationProfile`.

### Collections & Document Structures

#### `userGamificationProfiles` Collection

This central document stores a user's entire gamification state. The `_id` of this document will typically match the `userId` from the `muscle-user-service`.

````javascript
// Example Document:
{
  "_id": "user_id_from_muscle_user_service", // e.g., "6543210abcdef1234567890"
  "points": 1250,
  "level": 12,
  "lastLevelUpDate": ISODate("2025-07-01T15:00:00.000Z"),
  "streaks": {
    "dailyWorkout": { "current": 7, "lastUpdate": ISODate("2025-07-09T12:00:00Z") },
    "weeklyCardio": { "current": 3, "lastUpdate": ISODate("2025-07-08T10:00:00Z") }
  },
  "earnedBadges": [ // Array of earned badge instances (embedding UserBadge)
    {
      "badgeId": ObjectId("66928e1b0a1b2c3d4e5f6789"), // Reference to a Badge definition
      "earnedAt": ISODate("2025-06-15T09:00:00Z")
    },
    {
      "badgeId": ObjectId("66928e1b0a1b2c3d4e5f6790"),
      "earnedAt": ISODate("2025-07-01T11:30:00Z")
    }
  ],
  "currentQuests": [ // Array of active/recent user quest progress (embedding UserQuest data)
    {
      "questId": ObjectId("66928e1b0a1b2c3d4e5f6791"), // Reference to a Quest definition
      "objectiveProgress": 5, // e.g., 5 reps completed out of 10 target
      "status": "in_progress", // enum: 'in_progress', 'completed', 'failed'
      "startDate": ISODate("2025-07-08T00:00:00Z"),
      "completionDate": null, // Set when completed
      "createdAt": ISODate("2025-07-08T08:00:00Z")
    },
    {
      "questId": ObjectId("66928e1b0a1b2c3d4e5f6792"),
      "objectiveProgress": 30,
      "status": "completed",
      "startDate": ISODate("2025-07-05T00:00:00Z"),
      "completionDate": ISODate("2025-07-05T18:00:00Z"),
      "createdAt": ISODate("2025-07-05T08:00:00Z")
    }
  ],
  "createdAt": ISODate("2025-01-01T00:00:00.000Z"),
  "updatedAt": ISODate("2025-07-09T14:35:00.000Z")
}
````


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
````

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
