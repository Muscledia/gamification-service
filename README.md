# Gamification Service

This microservice is a core component of the Muscledia ecosystem, dedicated to enhancing user engagement and motivation through gamification. It tracks user progress, awards points, manages levels, and grants badges based on activity events from other services, primarily the `workout-service`.

## Table of Contents

1.  [Overview](#1-overview)
2.  [Responsibilities](#2-responsibilities)
3.  [Technology Stack](#3-technology-stack)
4.  [Core Entities / Data Model](#4-core-entities--data-model)
5.  [Event-Driven Architecture](#5-event-driven-architecture)
    * [Events Consumed](#events-consumed)
    * [Events Published](#events-published-future-consideration)
6.  [API Endpoints](#6-api-endpoints)
7.  [Getting Started](#7-getting-started)
    * [Prerequisites](#prerequisites)
    * [Running Locally](#running-locally)
    * [Configuration](#configuration)
8.  [Development Roadmap](#8-development-roadmap)
9.  [Future Enhancements](#9-future-enhancements)

---

## 1. Overview

The `gamification-service` operates on an event-driven model, ensuring loose coupling with other services. It consumes relevant user activity events, processes them against defined gamification rules, and updates the user's gamified profile in real-time. This service provides the necessary APIs for the frontend to display a user's achievements and global leaderboards.

## 2. Responsibilities

* **Event Subscription:** Listen for and consume specific user activity events (e.g., `WorkoutCompletedEvent`, `ExerciseLoggedEvent`) from **Kafka topics**.
* **Gamification Rule Processing:** Evaluate incoming events against a predefined set of rules to determine eligibility for points, level progression, and badge awards.
* **User State Management:** Persist and update users' current points, levels, earned badges, streaks, and other progress metrics within its dedicated database.
* **Achievement Query APIs:** Expose robust RESTful endpoints for querying a user's comprehensive gamification profile, specific earned badges, and various leaderboard data.
* **Achievement Event Publishing (Future):** Potentially publish events (e.g., `AchievementEarnedEvent`) to notify other services (like a `notification-service`) about significant user achievements via **Kafka topics**.

## 3. Technology Stack

* **Language:** Java
* **Framework:** Spring Boot (Leveraging Spring WebFlux for reactive and non-blocking operations)
* **Database:** MongoDB (Chosen for its flexible document model, ideal for varied gamification data, and available free tiers like MongoDB Atlas M0).
* **Messaging:** **Apache Kafka** for robust, high-throughput, and durable asynchronous event communication.
    * **Libraries:** **Spring Kafka**.
* **Build Tool:** Maven (or Gradle, ensuring consistency with other Muscledia microservices).
* **Dependencies:** Spring Boot Starters for Reactive Web (WebFlux), MongoDB Reactive, **Kafka**, and Lombok for boilerplate reduction.

## 4. Core Entities / Data Model

The `gamification-service` will primarily manage the following entities within MongoDB. This leverages MongoDB's document-oriented nature by using collections for static definitions and embedding user-specific data within the `UserGamificationProfile`.

### Collections & Document Structures

#### `userGamificationProfiles` Collection

This central document stores a user's entire gamification state. The `_id` of this document will typically match the `userId` from the `muscle-user-service`.

```javascript
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
