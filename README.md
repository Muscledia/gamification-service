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

* **Event Subscription:** Listen for and consume specific user activity events (e.g., `WorkoutCompletedEvent`, `ExerciseLoggedEvent`) from the message broker.
* **Gamification Rule Processing:** Evaluate incoming events against a predefined set of rules to determine eligibility for points, level progression, and badge awards.
* **User State Management:** Persist and update users' current points, levels, earned badges, streaks, and challenge progress within its dedicated database.
* **Achievement Query APIs:** Expose robust RESTful endpoints for querying a user's comprehensive gamification profile, specific earned badges, and various leaderboard data.
* **Achievement Event Publishing (Future):** Potentially publish events (e.g., `AchievementEarnedEvent`) to notify other services (like a `notification-service`) about significant user achievements.

## 3. Technology Stack

* **Language:** Java
* **Framework:** Spring Boot (Leveraging Spring WebFlux for reactive and non-blocking operations)
* **Database:** MongoDB (Chosen for its flexible document model, ideal for varied gamification data, and available free tiers like MongoDB Atlas M0).
* **Messaging:** RabbitMQ (or Kafka) for robust, asynchronous inter-service communication.
    * **Libraries:** Spring AMQP (for RabbitMQ) or Spring Kafka.
* **Build Tool:** Maven (or Gradle, ensuring consistency with other Muscledia microservices).
* **Dependencies:** Spring Boot Starters for Reactive Web (WebFlux), MongoDB Reactive, AMQP (or Kafka), and Lombok for boilerplate reduction.

## 4. Core Entities / Data Model

The primary entities managed by this service within MongoDB include:

* **`UserGamificationProfile`**: Represents a user's overall gamification status.
    * `id`: `String` (Corresponds to the `userId` from `muscle-user-service`).
    * `points`: `Integer` - Total accumulated points.
    * `level`: `Integer` - Current gamification level.
    * `lastLevelUpDate`: `Instant` - Timestamp of the last level achievement.
    * `badges`: `List<UserBadge>` - Embedded documents or references to `UserBadge` to track earned badges.
    * `streaks`: `Map<String, Integer>` - Tracks various streak metrics (e.g., `dailyWorkoutStreak`, `weeklyExerciseStreak`).
    * `challengeProgress`: `Map<String, ChallengeProgress>` - Tracks progress for ongoing challenges.
    * `createdAt`: `Instant` - Timestamp of profile creation.
    * `updatedAt`: `Instant` - Timestamp of the last profile update.

* **`Badge`**: Defines the characteristics and criteria of an awardable badge.
    * `id`: `String` - Unique identifier for the badge itself.
    * `name`: `String` - Display name (e.g., "First Step", "Iron Lifter").
    * `description`: `String` - Detailed description of how the badge is earned.
    * `imageUrl`: `String` - URL to the badge icon/image.
    * `criteria`: `Map<String, Object>` - Flexible JSON/Map representing the rules for earning (e.g., `{ "type": "workoutCount", "value": 10, "period": "allTime" }`).
    * `pointsAwarded`: `Integer` - Points granted upon earning this badge.

* **`UserBadge`**: (Typically embedded within `UserGamificationProfile`'s `badges` list) Represents an instance of a badge earned by a user.
    * `badgeId`: `String` - Reference to the `Badge` entity's `id`.
    * `earnedAt`: `Instant` - Timestamp when the badge was earned.

## 5. Event-Driven Architecture

The `gamification-service` is designed as an event consumer, promoting loose coupling within the microservice ecosystem.

### Events Consumed

This service subscribes to messages from the central message broker. Each event contains minimal, necessary data to trigger gamification logic.

* **From `workout-service`:**
    * `WorkoutCompletedEvent`: Published when a user successfully logs a completed workout.
        * **Payload Example:** `{ "userId": "user123", "workoutId": "wk001", "durationSeconds": 3600, "totalWeightLiftedKg": 500, "completedAt": "2023-10-27T10:00:00Z" }`
    * `ExerciseLoggedEvent`: Published when a user logs a specific exercise within a workout.
        * **Payload Example:** `{ "userId": "user123", "exerciseId": "ex001", "reps": 10, "sets": 3, "weight": 50, "loggedAt": "2023-10-27T10:15:00Z" }`
    * `PersonalBestAchievedEvent` (Optional, if detected by `workout-service`):
        * **Payload Example:** `{ "userId": "user123", "exerciseId": "ex005", "metricType": "weight", "value": 150, "achievedAt": "2023-10-27T10:30:00Z" }`

### Events Published (Future Consideration)

As the service evolves, it might publish its own events for other services to consume:

* **To `notification-service`:**
    * `AchievementEarnedEvent`: Notifies when a user reaches a significant milestone.
        * **Payload Example:** `{ "userId": "user123", "achievementType": "LEVEL_UP", "details": { "level": 5, "pointsEarned": 100 } }` or `{ "userId": "user123", "achievementType": "BADGE_EARNED", "details": { "badgeId": "badge001", "badgeName": "Consistent Contender" } }`

## 6. API Endpoints

The `gamification-service` will expose the following RESTful endpoints to allow frontends and other services to query gamification data:

* `GET /api/v1/gamification/users/{userId}/profile`
    * Retrieves a comprehensive view of a user's gamification profile (points, level, current streaks, etc.).
* `GET /api/v1/gamification/users/{userId}/badges`
    * Lists all badges earned by a specific user.
* `GET /api/v1/gamification/leaderboard/points`
    * Provides a global leaderboard ordered by total points.
* `GET /api/v1/gamification/badges`
    * Retrieves a list of all defined badges with their descriptions and criteria.

## 7. Getting Started

### Prerequisites

* **Java:** JDK 17 or higher.
* **Build Tool:** Maven.
* **Docker:** Recommended for easily running local instances of MongoDB and RabbitMQ.

### Running Locally

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd gamification-service
    ```
2.  **Start Dependencies with Docker Compose:**
    Ensure Docker is running. Create a `docker-compose.yml` in the project root:
    ```yaml
    version: '3.8'
    services:
      mongodb:
        image: mongo:latest
        container_name: mongodb_gamification
        ports:
          - "27017:27017"
        volumes:
          - mongodb_gamification_data:/data/db

      rabbitmq:
        image: rabbitmq:management
        container_name: rabbitmq_gamification
        ports:
          - "5672:5672" # AMQP protocol port
          - "15672:15672" # Management UI port
        environment:
          RABBITMQ_DEFAULT_USER: guest
          RABBITMQ_DEFAULT_PASS: guest
        healthcheck:
          test: ["CMD", "rabbitmqctl", "status"]
          interval: 5s
          timeout: 10s
          retries: 5
          start_period: 20s

    volumes:
      mongodb_gamification_data:
    ```
    Then, from your terminal in the same directory:
    ```bash
    docker-compose up -d
    ```

3.  **Configure `application.yml`:**
    Edit `src/main/resources/application.yml` to match your local or cloud MongoDB/RabbitMQ setup.

    ```yaml
    # application.yml
    spring:
      application:
        name: gamification-service
      data:
        mongodb:
          uri: mongodb://localhost:27017/gamificationdb # For local Docker
          # For MongoDB Atlas: Uncomment and replace with your actual connection string
          # uri: ${MONGO_ATLAS_URI:mongodb+srv://user:pass@cluster.mongodb.net/gamificationdb?retryWrites=true&w=majority}
      rabbitmq:
        host: localhost # For local Docker
        port: 5672
        username: guest
        password: guest

    # Custom application properties for message queue configurations
    muscledia:
      mq:
        exchanges:
          workout: workout-exchange # Name of the exchange in RabbitMQ
        routing-keys:
          workout-completed: workout.completed # Routing key for workout completion events
          exercise-logged: exercise.logged # Routing key for exercise logging events
          personal-best-achieved: personal.best.achieved # Routing key for personal bests
        queues:
          workout-completed: gamification-workout-completed-queue # Queue for this service to listen to workout completions
          exercise-logged: gamification-exercise-logged-queue # Queue for this service to listen to exercise logs
          personal-best-achieved: gamification-personal-best-achieved-queue # Queue for personal bests
    ```
    _Remember to create the queues and exchange in RabbitMQ, or configure Spring AMQP to do so automatically upon startup._

4.  **Build and Run the Service:**
    ```bash
    ./mvnw clean install
    ./mvnw spring-boot:run
    ```

### Configuration

Sensitive configurations like database credentials or API keys should be externalized using environment variables (e.g., `MONGO_ATLAS_URI`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`) or a dedicated secret management solution for production deployments.

## 8. Development Roadmap

This section outlines the initial phases of developing the `gamification-service`:

1.  **Project Setup (Done):** Initialize Spring Boot project, add necessary dependencies (`spring-boot-starter-webflux`, `spring-boot-starter-data-mongodb-reactive`, `spring-boot-starter-amqp`, `lombok`).
2.  **Core Data Models:** Define `UserGamificationProfile`, `Badge`, and `UserBadge` MongoDB entities (`@Document`).
3.  **Database Integration:** Configure Spring Data MongoDB reactive repositories for `UserGamificationProfile` and `Badge`.
4.  **RabbitMQ Consumer Setup:**
    * Configure `RabbitListenerContainerFactory` and `MessageConverter`.
    * Implement `@RabbitListener` methods to consume `WorkoutCompletedEvent` and `ExerciseLoggedEvent`.
    * Define RabbitMQ Queues, Exchanges, and Bindings via `@Bean` configurations in a dedicated `RabbitMQConfig` class for auto-creation.
5.  **Basic Gamification Logic:**
    * **Points Calculation:** Implement logic within event listeners to calculate and update `points` in `UserGamificationProfile` based on event data (e.g., points per workout, points per kg lifted).
    * **Level Progression:** Implement rules for `level` increases based on total `points` accumulated.
6.  **Basic Badge System:**
    * Seed initial `Badge` definitions into MongoDB.
    * Implement logic to check and award simple badges (e.g., "First Workout," "5 Workouts Completed") to users, adding them to their `UserGamificationProfile`.
7.  **REST APIs:**
    * Create a `GamificationController` with endpoints to:
        * `GET /api/v1/gamification/users/{userId}/profile`
        * `GET /api/v1/gamification/users/{userId}/badges`
    * Implement initial service logic for these APIs.

## 9. Future Enhancements

* **Advanced Badge & Challenge System:** Support for more complex criteria, multi-stage challenges, time-bound challenges, and exercise-specific achievements.
* **Leaderboards:** Develop more sophisticated leaderboard functionalities (e.g., global, weekly, friends-based, real-time updates using Redis sorted sets).
* **Notification Integration:** Implement publishing of `AchievementEarnedEvent` to a dedicated notification service for in-app or push notifications.
* **Admin Interface/APIs:** Develop a management interface or dedicated APIs for administrators to define, modify, and manage gamification rules, badges, and challenges.
* **Real-time Frontend Updates:** Explore using WebSockets or Server-Sent Events (SSE) to push gamification updates to connected client applications.
* **Scalability & Performance:** Further optimize MongoDB queries, potentially explore sharding for very large datasets, and use Redis for caching hot data.

---
