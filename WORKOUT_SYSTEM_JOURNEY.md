# 🏋️ Complete Workout System Journey

## Overview

This document provides a comprehensive walkthrough of the workout system in Muscledia, from exploring routines to completing workouts and tracking personal records.

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Exploring Routines](#exploring-routines)
3. [Creating a Routine](#creating-a-routine)
4. [Starting a Workout](#starting-a-workout)
5. [During Workout - Set Logging](#during-workout---set-logging)
6. [Personal Record Detection](#personal-record-detection)
7. [Completing a Workout](#completing-a-workout)
8. [CRUD Operations](#crud-operations)
9. [Database Schema](#database-schema)
10. [API Reference](#api-reference)

---

## System Architecture
```
┌─────────────────────────────────────────────────────────────┐
│              FRONTEND (React Native Expo)                   │
│                                                              │
│  Screens:                                                    │
│  • Routine Explorer                                          │
│  • Routine Folders                                           │
│  • Routine Detail                                            │
│  • Exercise Selector                                         │
│  • Active Workout                                            │
│  • Workout History                                           │
└─────────────────────────────────────────────────────────────┘
                          ↕ REST API
┌─────────────────────────────────────────────────────────────┐
│                    API GATEWAY (8080)                       │
│  • JWT Authentication                                        │
│  • Request Routing                                           │
│  • CORS Handling                                             │
└─────────────────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────────────────┐
│                 WORKOUT SERVICE (8081)                      │
│                                                              │
│  Controllers:                                                │
│  • RoutineFolderController                                   │
│  • RoutineController                                         │
│  • WorkoutController                                         │
│  • WorkoutSetController                                      │
│  • ExerciseController                                        │
│  • PersonalRecordController                                  │
│                                                              │
│  Services:                                                   │
│  • RoutineFolderService                                      │
│  • RoutineService                                            │
│  • WorkoutSessionService                                     │
│  • WorkoutSetService                                         │
│  • PersonalRecordService                                     │
│  • WorkoutCompletionService                                  │
│                                                              │
│  Repositories:                                               │
│  • RoutineFolderRepository (MySQL)                           │
│  • RoutineRepository (MySQL)                                 │
│  • WorkoutSessionRepository (MongoDB)                        │
│  • WorkoutSetRepository (MongoDB)                            │
│  • ExerciseRepository (MySQL)                                │
│  • PersonalRecordRepository (MongoDB)                        │
└─────────────────────────────────────────────────────────────┘
        ↓                    ↓                    ↓
     MySQL              MongoDB              Apache Kafka
  (Structured)         (Documents)         (Event Streaming)
```

---

## Exploring Routines

### User Opens Routine Screen

**Frontend Flow:**
```
App Launch
    ↓
Navigate to "Workout" tab
    ↓
RoutineExplorerScreen loads
```

**Backend Request 1: Get Routine Folders**
```http
GET /api/routine-folders/user
Authorization: Bearer <JWT_TOKEN>

Response:
{
  "data": [
    {
      "id": 1,
      "name": "Upper Body",
      "description": "Chest, back, shoulders",
      "routineCount": 3,
      "createdAt": "2025-12-01T00:00:00Z"
    },
    {
      "id": 2,
      "name": "Lower Body",
      "description": "Legs and glutes",
      "routineCount": 2,
      "createdAt": "2025-12-01T00:00:00Z"
    }
  ]
}
```

**Backend Processing:**
```
RoutineFolderController.getUserRoutineFolders()
    ↓
Extract userId from JWT (SecurityContextUtil.getCurrentUserId())
    ↓
RoutineFolderService.getUserFolders(userId)
    ↓
RoutineFolderRepository.findByUserId(userId)
    ↓
For each folder, count routines:
  routineRepository.countByFolderId(folderId)
    ↓
Map to DTO and return
```

**Frontend Rendering:**
```tsx
// Routine Folders Screen
<FolderGrid>
  <FolderCard onPress={() => navigate('FolderDetail', { folderId: 1 })}>
    <Icon>📁</Icon>
    <Title>Upper Body</Title>
    <Subtitle>3 routines</Subtitle>
  </FolderCard>
  
  <FolderCard onPress={() => navigate('FolderDetail', { folderId: 2 })}>
    <Icon>📁</Icon>
    <Title>Lower Body</Title>
    <Subtitle>2 routines</Subtitle>
  </FolderCard>
</FolderGrid>
```

### User Taps on a Folder

**Frontend Request:**
```http
GET /api/routines/folder/{folderId}
Authorization: Bearer <JWT_TOKEN>

Response:
{
  "data": [
    {
      "id": 101,
      "name": "Push Day",
      "description": "Chest, shoulders, triceps",
      "exerciseCount": 6,
      "estimatedDuration": 60,
      "lastPerformed": "2025-12-20T10:00:00Z",
      "totalWorkouts": 5
    },
    {
      "id": 102,
      "name": "Pull Day",
      "description": "Back and biceps",
      "exerciseCount": 5,
      "estimatedDuration": 50,
      "lastPerformed": null,
      "totalWorkouts": 0
    }
  ]
}
```

**Backend Processing:**
```
RoutineController.getRoutinesByFolder(folderId)
    ↓
Validate folder ownership:
  RoutineFolderRepository.existsByIdAndUserId(folderId, userId)
    ↓
RoutineService.getRoutinesByFolder(folderId)
    ↓
RoutineRepository.findByFolderId(folderId)
    ↓
For each routine:
  • Count exercises: plannedExercises.size()
  • Get last workout: workoutSessionRepository.findMostRecentByRoutineId()
  • Count total workouts: workoutSessionRepository.countByRoutineId()
    ↓
Map to RoutineDto and return
```

---

## Creating a Routine

### User Taps "Create New Routine"

**Frontend Flow:**
```
RoutineExplorerScreen
    ↓
Tap "+" button
    ↓
Navigate to CreateRoutineScreen
```

**Step 1: Basic Info**
```tsx
<CreateRoutineForm>
  <Input
    label="Routine Name"
    placeholder="e.g., Push Day"
    value={name}
    onChange={setName}
  />
  
  <Input
    label="Description"
    placeholder="What does this routine target?"
    value={description}
    onChange={setDescription}
    multiline
  />
  
  <FolderSelector
    selectedFolder={folderId}
    onSelect={setFolderId}
  />
  
  <Button onPress={saveRoutine}>
    Create Routine
  </Button>
</CreateRoutineForm>
```

**Backend Request:**
```http
POST /api/routines
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "name": "Push Day",
  "description": "Chest, shoulders, triceps",
  "folderId": 1
}

Response:
{
  "success": true,
  "data": {
    "id": 103,
    "name": "Push Day",
    "description": "Chest, shoulders, triceps",
    "folderId": 1,
    "exerciseCount": 0,
    "createdAt": "2025-12-22T10:00:00Z"
  }
}
```

**Backend Processing:**
```
RoutineController.createRoutine(CreateRoutineRequest)
    ↓
Extract userId from JWT
    ↓
Validate folder exists and belongs to user:
  RoutineFolderRepository.existsByIdAndUserId(folderId, userId)
    ↓
RoutineService.createRoutine(request, userId)
    ↓
Create Routine entity:
  Routine.builder()
    .name("Push Day")
    .description("...")
    .folderId(1)
    .userId(userId)
    .build()
    ↓
Save to MySQL routines table
    ↓
Return RoutineDto
```

**Database State:**
```sql
-- MySQL: routines table
INSERT INTO routines (
  id, user_id, folder_id, name, description, created_at
) VALUES (
  103, 3602951135283784, 1, 'Push Day', 'Chest, shoulders, triceps', NOW()
);
```

### Step 2: Add Exercises to Routine

**Frontend Flow:**
```
RoutineDetailScreen (routineId: 103)
    ↓
Tap "Add Exercise"
    ↓
ExerciseSelectorScreen with search/filter
```

**Exercise Selection Request:**
```http
GET /api/exercises?category=CHEST&page=0&size=20
Authorization: Bearer <JWT_TOKEN>

Response:
{
  "content": [
    {
      "id": 501,
      "name": "Bench Press",
      "category": "CHEST",
      "equipment": "BARBELL",
      "muscleGroups": ["CHEST", "TRICEPS", "SHOULDERS"],
      "instructions": "...",
      "gifUrl": "https://..."
    },
    {
      "id": 502,
      "name": "Incline Dumbbell Press",
      "category": "CHEST",
      "equipment": "DUMBBELL",
      "muscleGroups": ["CHEST", "SHOULDERS"],
      "instructions": "...",
      "gifUrl": "https://..."
    }
  ],
  "totalElements": 15,
  "totalPages": 1
}
```

**User Selects Exercise:**
```http
POST /api/routines/{routineId}/exercises
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "exerciseId": 501,
  "orderIndex": 0,
  "defaultSets": 4,
  "defaultReps": 10,
  "defaultWeight": 60.0,
  "restSeconds": 120,
  "notes": "Focus on form"
}

Response:
{
  "success": true,
  "data": {
    "id": 1001,
    "exerciseId": 501,
    "exerciseName": "Bench Press",
    "orderIndex": 0,
    "defaultSets": 4,
    "defaultReps": 10,
    "defaultWeight": 60.0,
    "restSeconds": 120
  }
}
```

**Backend Processing:**
```
RoutineController.addExerciseToRoutine(routineId, request)
    ↓
Validate routine ownership
    ↓
Validate exercise exists:
  ExerciseRepository.existsById(exerciseId)
    ↓
RoutineService.addPlannedExercise(routineId, request)
    ↓
Create PlannedExercise entity:
  PlannedExercise.builder()
    .routineId(routineId)
    .exerciseId(501)
    .exercise(exerciseRepository.findById(501))
    .orderIndex(0)
    .targetSets(4)
    .targetReps(10)
    .targetWeight(60.0)
    .restSeconds(120)
    .build()
    ↓
Save to planned_exercises table
    ↓
Return PlannedExerciseDto
```

**Repeat for More Exercises:**

User adds:
1. Bench Press (4x10, 60kg)
2. Incline Dumbbell Press (3x12, 25kg)
3. Cable Flyes (3x15, 15kg)
4. Overhead Press (4x8, 40kg)
5. Lateral Raises (3x12, 10kg)
6. Tricep Pushdowns (3x15, 20kg)

**Final Routine State:**
```
Routine: "Push Day"
├── Exercise 1: Bench Press (4x10 @ 60kg)
├── Exercise 2: Incline Dumbbell Press (3x12 @ 25kg)
├── Exercise 3: Cable Flyes (3x15 @ 15kg)
├── Exercise 4: Overhead Press (4x8 @ 40kg)
├── Exercise 5: Lateral Raises (3x12 @ 10kg)
└── Exercise 6: Tricep Pushdowns (3x15 @ 20kg)

Estimated Duration: 60 minutes
Total Exercises: 6
```

---

## Starting a Workout

### User Taps "Start Workout"

**Frontend Request:**
```http
POST /api/workouts/start
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "routineId": 103,
  "name": "Push Day - Morning Session"
}

Response:
{
  "success": true,
  "data": {
    "id": "workout-xyz789",
    "routineId": 103,
    "routineName": "Push Day",
    "userId": 3602951135283784,
    "name": "Push Day - Morning Session",
    "status": "IN_PROGRESS",
    "startTime": "2025-12-22T10:00:00Z",
    "exercises": [
      {
        "exerciseId": 501,
        "exerciseName": "Bench Press",
        "orderIndex": 0,
        "plannedSets": 4,
        "completedSets": 0,
        "targetReps": 10,
        "targetWeight": 60.0
      },
      // ... other exercises
    ]
  }
}
```

**Backend Processing:**
```
WorkoutController.startWorkout(StartWorkoutRequest)
    ↓
Extract userId from JWT
    ↓
WorkoutSessionService.startWorkout(request, userId)
    ↓
1. Get Routine:
   Routine routine = routineRepository.findById(routineId)
   
2. Validate ownership:
   if (routine.getUserId() != userId) throw ForbiddenException
   
3. Get Planned Exercises:
   List<PlannedExercise> exercises = routine.getPlannedExercises()
   
4. Create WorkoutSession document:
   WorkoutSession.builder()
     .id(UUID.randomUUID())
     .userId(userId)
     .routineId(routineId)
     .routineName("Push Day")
     .name("Push Day - Morning Session")
     .status(WorkoutStatus.IN_PROGRESS)
     .startTime(Instant.now())
     .plannedExercises(exercises)
     .workoutSets([]) // Empty initially
     .build()
   
5. Save to MongoDB workout_sessions collection
   
6. Return WorkoutSessionDto
```

**MongoDB Document Created:**
```javascript
{
  _id: "workout-xyz789",
  userId: 3602951135283784,
  routineId: 103,
  routineName: "Push Day",
  name: "Push Day - Morning Session",
  status: "IN_PROGRESS",
  startTime: ISODate("2025-12-22T10:00:00Z"),
  endTime: null,
  totalDurationSeconds: null,
  plannedExercises: [
    {
      exerciseId: 501,
      exerciseName: "Bench Press",
      orderIndex: 0,
      targetSets: 4,
      targetReps: 10,
      targetWeight: 60.0,
      restSeconds: 120,
      equipment: "BARBELL",
      muscleGroups: ["CHEST", "TRICEPS", "SHOULDERS"]
    },
    // ... 5 more exercises
  ],
  workoutSets: [], // Will be populated as user logs sets
  metrics: {
    totalSets: 0,
    totalReps: 0,
    totalVolume: 0.0,
    uniqueExercises: 0,
    personalRecordsAchieved: 0
  },
  createdAt: ISODate("2025-12-22T10:00:00Z"),
  updatedAt: ISODate("2025-12-22T10:00:00Z")
}
```

**Frontend Navigation:**
```tsx
// Navigate to active workout screen
navigation.navigate('ActiveWorkout', { workoutId: 'workout-xyz789' });
```

---

## During Workout - Set Logging

### Active Workout Screen Structure
```tsx
<ActiveWorkoutScreen>
  <WorkoutHeader>
    <Timer running={true} />
    <WorkoutName>Push Day - Morning Session</WorkoutName>
    <QuickActions>
      <PauseButton />
      <EndWorkoutButton />
    </QuickActions>
  </WorkoutHeader>
  
  <ExerciseList>
    {exercises.map((exercise, index) => (
      <ExerciseCard
        key={exercise.exerciseId}
        exercise={exercise}
        isActive={currentExerciseIndex === index}
      >
        <ExerciseName>{exercise.exerciseName}</ExerciseName>
        <SetGrid>
          {exercise.sets.map((set, setIndex) => (
            <SetRow
              key={setIndex}
              setNumber={setIndex + 1}
              reps={set.reps}
              weight={set.weight}
              isCompleted={set.completed}
              onLog={() => logSet(exercise, setIndex)}
            />
          ))}
          <AddSetButton onPress={() => addSet(exercise)} />
        </SetGrid>
      </ExerciseCard>
    ))}
  </ExerciseList>
</ActiveWorkoutScreen>
```

### User Logs First Set

**User Action:**
```
Exercise: Bench Press
Set 1: 60kg x 10 reps
Tap "Log Set" button
```

**Frontend Request:**
```http
POST /api/workouts/{workoutId}/sets
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "exerciseId": 501,
  "exerciseName": "Bench Press",
  "setNumber": 1,
  "setType": "WORKING",
  "reps": 10,
  "weight": 60.0,
  "rpe": 7.5,
  "isPersonalRecord": false
}

Response:
{
  "success": true,
  "data": {
    "id": "set-001",
    "workoutId": "workout-xyz789",
    "exerciseId": 501,
    "setNumber": 1,
    "setType": "WORKING",
    "reps": 10,
    "weight": 60.0,
    "volume": 600.0,
    "rpe": 7.5,
    "timestamp": "2025-12-22T10:05:00Z",
    "personalRecords": []
  }
}
```

**Backend Processing:**
```
WorkoutSetController.logSet(workoutId, LogSetRequest)
    ↓
Extract userId from JWT
    ↓
WorkoutSetService.logSet(workoutId, request, userId)
    ↓
1. Get WorkoutSession:
   WorkoutSession workout = workoutSessionRepository.findById(workoutId)
   
2. Validate:
   • workout.getUserId() == userId
   • workout.getStatus() == IN_PROGRESS
   
3. Create WorkoutSet:
   WorkoutSet set = WorkoutSet.builder()
     .id(UUID.randomUUID())
     .workoutId(workoutId)
     .exerciseId(501)
     .exerciseName("Bench Press")
     .setNumber(1)
     .setType(WORKING)
     .reps(10)
     .weight(60.0)
     .volume(600.0) // reps * weight
     .rpe(7.5)
     .timestamp(Instant.now())
     .build()
   
4. Check for Personal Records:
   PersonalRecordService.checkAndCreatePRs(userId, set)
     ↓
   Query PersonalRecordRepository:
     • MAX_WEIGHT for Bench Press
     • MAX_REPS for Bench Press @ 60kg
     • MAX_VOLUME for Bench Press
     • ESTIMATED_1RM for Bench Press
   
   If current set beats any existing PR:
     • Create new PersonalRecord document
     • Update set.personalRecords[]
     • Increment workout.metrics.personalRecordsAchieved
   
5. Add set to workout:
   workout.getWorkoutSets().add(set)
   
6. Update metrics:
   workout.getMetrics().setTotalSets(workout.getMetrics().getTotalSets() + 1)
   workout.getMetrics().setTotalReps(workout.getMetrics().getTotalReps() + 10)
   workout.getMetrics().setTotalVolume(workout.getMetrics().getTotalVolume() + 600.0)
   
7. Save workout to MongoDB
   
8. Return WorkoutSetDto
```

**Updated Workout Document:**
```javascript
{
  _id: "workout-xyz789",
  // ... other fields
  workoutSets: [
    {
      _id: "set-001",
      exerciseId: 501,
      exerciseName: "Bench Press",
      setNumber: 1,
      setType: "WORKING",
      reps: 10,
      weight: 60.0,
      volume: 600.0,
      rpe: 7.5,
      timestamp: ISODate("2025-12-22T10:05:00Z"),
      personalRecords: []
    }
  ],
  metrics: {
    totalSets: 1,
    totalReps: 10,
    totalVolume: 600.0,
    uniqueExercises: 1,
    personalRecordsAchieved: 0
  }
}
```

### Rest Timer

**Frontend Auto-Start:**
```tsx
const onSetLogged = (set) => {
  // Start rest timer automatically
  const restDuration = exercise.restSeconds || 120; // 2 minutes
  startRestTimer(restDuration);
  
  // Show rest timer overlay
  showRestTimer({
    duration: restDuration,
    nextSet: set.setNumber + 1,
    onComplete: () => {
      // Vibrate + sound notification
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    }
  });
};
```

**Rest Timer UI:**
```tsx
<RestTimerOverlay>
  <CircularProgress value={timeRemaining} max={restDuration} />
  <TimeDisplay>{formatTime(timeRemaining)}</TimeDisplay>
  <NextSetInfo>
    Next: Set {nextSetNumber} • {targetReps} reps @ {targetWeight}kg
  </NextSetInfo>
  <SkipButton onPress={skipRest}>Skip Rest</SkipButton>
</RestTimerOverlay>
```

---

## Personal Record Detection

### Automatic PR Detection

**When Set is Logged:**
```
PersonalRecordService.checkAndCreatePRs(userId, workoutSet)
    ↓
For each PR type:
  • MAX_WEIGHT
  • MAX_REPS
  • MAX_VOLUME
  • ESTIMATED_1RM
  
1. Query existing PR:
   PersonalRecordRepository.findByUserIdAndExerciseIdAndType(
     userId, exerciseId, prType
   )
   
2. Calculate current value:
   switch (prType) {
     case MAX_WEIGHT:
       currentValue = workoutSet.getWeight()
     case MAX_REPS:
       currentValue = workoutSet.getReps()
     case MAX_VOLUME:
       currentValue = workoutSet.getVolume()
     case ESTIMATED_1RM:
       currentValue = calculate1RM(weight, reps)
   }
   
3. Compare with existing PR:
   if (existingPR == null || currentValue > existingPR.getValue()) {
     // NEW PERSONAL RECORD! 🎉
     createNewPR()
   }
```

**1RM Calculation (Epley Formula):**
```java
private double calculate1RM(double weight, int reps) {
    if (reps == 1) {
        return weight;
    }
    return weight * (1 + (reps / 30.0));
}

// Example:
// 60kg x 10 reps = 60 * (1 + 10/30) = 60 * 1.333 = 80kg estimated 1RM
```

**Creating Personal Record:**
```
PersonalRecordService.createNewPR()
    ↓
PersonalRecord pr = PersonalRecord.builder()
  .userId(userId)
  .exerciseId(501)
  .exerciseName("Bench Press")
  .recordType(MAX_WEIGHT)
  .value(60.0)
  .reps(10)
  .workoutId(workoutId)
  .workoutSetId(setId)
  .achievedAt(Instant.now())
  .previousRecord(existingPR != null ? existingPR.getValue() : null)
  .improvement(existingPR != null ? value - existingPR.getValue() : value)
  .build()
    ↓
Save to personal_records collection
    ↓
Update workoutSet.personalRecords.add(pr)
    ↓
Increment workout.metrics.personalRecordsAchieved
```

**PR Celebration in Frontend:**
```tsx
const onPRDetected = (personalRecord) => {
  // Show immediate celebration
  showPRModal({
    exerciseName: personalRecord.exerciseName,
    recordType: personalRecord.recordType,
    newValue: personalRecord.value,
    previousValue: personalRecord.previousRecord,
    improvement: personalRecord.improvement
  });
  
  // Haptic feedback
  Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
  
  // Confetti animation
  triggerConfetti();
};
```

**PR Modal UI:**
```tsx
<PRCelebrationModal>
  <FireAnimation />
  <Emoji>🏆</Emoji>
  <Title>New Personal Record!</Title>
  
  <ExerciseName>Bench Press</ExerciseName>
  <RecordType>Max Weight</RecordType>
  
  <ValueComparison>
    <OldValue>55kg</OldValue>
    <Arrow>→</Arrow>
    <NewValue>60kg</NewValue>
  </ValueComparison>
  
  <Improvement>+5kg improvement!</Improvement>
</PRCelebrationModal>
```

---

## Completing a Workout

### User Finishes All Sets

**User taps "End Workout":**
```tsx
const endWorkout = async () => {
  // Confirm dialog
  const confirmed = await showConfirmDialog({
    title: "End Workout?",
    message: "Are you sure you want to finish this workout?",
    confirmText: "End Workout",
    cancelText: "Continue"
  });
  
  if (confirmed) {
    completeWorkout();
  }
};
```

**Frontend Request:**
```http
POST /api/workouts/{workoutId}/complete
Authorization: Bearer <JWT_TOKEN>

Response:
{
  "success": true,
  "data": {
    "id": "workout-xyz789",
    "status": "COMPLETED",
    "startTime": "2025-12-22T10:00:00Z",
    "endTime": "2025-12-22T10:45:00Z",
    "durationMinutes": 45,
    "metrics": {
      "totalSets": 22,
      "totalReps": 230,
      "totalVolume": 4250.5,
      "uniqueExercises": 6,
      "personalRecordsAchieved": 2
    },
    "personalRecords": [
      {
        "exerciseName": "Bench Press",
        "recordType": "MAX_WEIGHT",
        "value": 60.0,
        "improvement": 5.0
      },
      {
        "exerciseName": "Overhead Press",
        "recordType": "ESTIMATED_1RM",
        "value": 53.3,
        "improvement": 3.3
      }
    ]
  }
}
```

**Backend Processing:**
```
WorkoutController.completeWorkout(workoutId)
    ↓
Extract userId from JWT
    ↓
WorkoutCompletionService.completeWorkout(workoutId, userId)
    ↓
1. Get WorkoutSession:
   WorkoutSession workout = workoutSessionRepository.findById(workoutId)
   
2. Validate:
   • workout.getUserId() == userId
   • workout.getStatus() == IN_PROGRESS
   
3. Update workout:
   workout.setStatus(COMPLETED)
   workout.setEndTime(Instant.now())
   
4. Calculate duration:
   Duration duration = Duration.between(workout.getStartTime(), workout.getEndTime())
   workout.setTotalDurationSeconds(duration.getSeconds())
   
5. Calculate final metrics:
   WorkoutMetrics metrics = calculateMetrics(workout.getWorkoutSets())
   workout.setMetrics(metrics)
   
6. Save workout to MongoDB
   
7. Publish WorkoutCompletedEvent to Kafka:
   WorkoutCompletedEvent event = WorkoutCompletedEvent.builder()
     .userId(userId)
     .workoutId(workoutId)
     .workoutType("strength")
     .durationMinutes(45)
     .totalReps(230)
     .totalSets(22)
     .exercisesCompleted(6)
     .totalVolume(4250.5)
     .caloriesBurned(calculateCalories(metrics))
     .personalRecordsAchieved(2)
     .timestamp(Instant.now())
     .build()
   
   kafkaTemplate.send("workout-events", event)
   
8. Return WorkoutSummaryDto
```

**Kafka Event Published:**
```json
{
  "userId": 3602951135283784,
  "workoutId": "workout-xyz789",
  "workoutType": "strength",
  "durationMinutes": 45,
  "totalReps": 230,
  "totalSets": 22,
  "exercisesCompleted": 6,
  "totalVolume": 4250.5,
  "caloriesBurned": 320,
  "personalRecordsAchieved": 2,
  "workedMuscleGroups": ["CHEST", "SHOULDERS", "TRICEPS"],
  "workoutStartTime": "2025-12-22T10:00:00Z",
  "workoutEndTime": "2025-12-22T10:45:00Z",
  "timestamp": "2025-12-22T10:45:00Z"
}
```

**This triggers Gamification Service processing** (see Challenge System Journey)

---

## CRUD Operations

### Routine Folders

#### Create Folder
```http
POST /api/routine-folders
{
  "name": "Full Body",
  "description": "Total body workouts"
}
```

#### Update Folder
```http
PUT /api/routine-folders/{folderId}
{
  "name": "Full Body (Updated)",
  "description": "New description"
}
```

#### Delete Folder
```http
DELETE /api/routine-folders/{folderId}

Note: Cannot delete folder with routines inside
```

#### Move Routine to Different Folder
```http
PUT /api/routines/{routineId}/folder
{
  "folderId": 2
}
```

### Routines

#### Get Routine Details
```http
GET /api/routines/{routineId}

Response includes:
- Basic info
- All planned exercises
- Last performed date
- Total workout count
- Average duration
```

#### Update Routine
```http
PUT /api/routines/{routineId}
{
  "name": "Updated name",
  "description": "Updated description"
}
```

#### Delete Routine
```http
DELETE /api/routines/{routineId}

Cascades to delete all planned_exercises
```

#### Duplicate Routine
```http
POST /api/routines/{routineId}/duplicate
{
  "newName": "Push Day Copy",
  "folderId": 1
}

Creates new routine with all exercises copied
```

### Planned Exercises

#### Reorder Exercises
```http
PUT /api/routines/{routineId}/exercises/reorder
{
  "exerciseIds": [502, 501, 503, 504, 505, 506]
}

Updates orderIndex for all exercises
```

#### Update Exercise Settings
```http
PUT /api/routines/{routineId}/exercises/{plannedExerciseId}
{
  "defaultSets": 5,
  "defaultReps": 8,
  "defaultWeight": 65.0,
  "restSeconds": 180,
  "notes": "Increase weight next time"
}
```

#### Remove Exercise
```http
DELETE /api/routines/{routineId}/exercises/{plannedExerciseId}

Reorders remaining exercises
```

### Workouts

#### Get Workout History
```http
GET /api/workouts/history?page=0&size=20

Response:
{
  "content": [
    {
      "id": "workout-xyz789",
      "name": "Push Day - Morning Session",
      "startTime": "2025-12-22T10:00:00Z",
      "durationMinutes": 45,
      "totalVolume": 4250.5,
      "personalRecordsAchieved": 2
    }
  ],
  "totalElements": 15
}
```

#### Get Specific Workout
```http
GET /api/workouts/{workoutId}

Returns complete workout with all sets
```

#### Delete Workout
```http
DELETE /api/workouts/{workoutId}

Soft delete (marks as deleted, doesn't actually remove)
```

### Personal Records

#### Get All PRs for User
```http
GET /api/personal-records

Response:
{
  "data": [
    {
      "exerciseId": 501,
      "exerciseName": "Bench Press",
      "records": {
        "MAX_WEIGHT": {
          "value": 60.0,
          "reps": 10,
          "achievedAt": "2025-12-22T10:05:00Z"
        },
        "MAX_REPS": {
          "value": 12,
          "weight": 55.0,
          "achievedAt": "2025-12-20T14:30:00Z"
        },
        "MAX_VOLUME": {
          "value": 720.0,
          "reps": 12,
          "weight": 60.0,
          "achievedAt": "2025-12-22T10:05:00Z"
        },
        "ESTIMATED_1RM": {
          "value": 80.0,
          "reps": 10,
          "weight": 60.0,
          "achievedAt": "2025-12-22T10:05:00Z"
        }
      }
    }
  ]
}
```

#### Get PRs for Specific Exercise
```http
GET /api/personal-records/exercise/{exerciseId}
```

#### Get PR History (Timeline)
```http
GET /api/personal-records/exercise/{exerciseId}/history?type=MAX_WEIGHT

Shows progression over time
```

---

## Database Schema

### MySQL (Structured Data)

#### routine_folders
```sql
CREATE TABLE routine_folders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id)
);
```

#### routines
```sql
CREATE TABLE routines (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  folder_id BIGINT,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_folder_id (folder_id),
  FOREIGN KEY (folder_id) REFERENCES routine_folders(id) ON DELETE SET NULL
);
```

#### planned_exercises
```sql
CREATE TABLE planned_exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  routine_id BIGINT NOT NULL,
  exercise_id BIGINT NOT NULL,
  order_index INT NOT NULL,
  target_sets INT DEFAULT 3,
  target_reps INT DEFAULT 10,
  target_weight DECIMAL(10,2),
  rest_seconds INT DEFAULT 120,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  -- Denormalized exercise data for performance
  exercise_name VARCHAR(255),
  exercise_category VARCHAR(50),
  exercise_equipment VARCHAR(50),
  
  INDEX idx_routine_id (routine_id),
  INDEX idx_exercise_id (exercise_id),
  FOREIGN KEY (routine_id) REFERENCES routines(id) ON DELETE CASCADE
);
```

#### exercises
```sql
CREATE TABLE exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(50),
  equipment VARCHAR(50),
  instructions TEXT,
  gif_url VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### MongoDB (Document Data)

#### workout_sessions
```javascript
{
  _id: "workout-xyz789",
  userId: 3602951135283784,
  routineId: 103,
  routineName: "Push Day",
  name: "Push Day - Morning Session",
  status: "COMPLETED", // IN_PROGRESS, COMPLETED, CANCELLED
  startTime: ISODate("2025-12-22T10:00:00Z"),
  endTime: ISODate("2025-12-22T10:45:00Z"),
  totalDurationSeconds: 2700,
  
  plannedExercises: [
    {
      exerciseId: 501,
      exerciseName: "Bench Press",
      orderIndex: 0,
      targetSets: 4,
      targetReps: 10,
      targetWeight: 60.0,
      restSeconds: 120,
      equipment: "BARBELL",
      muscleGroups: ["CHEST", "TRICEPS", "SHOULDERS"],
      category: "CHEST"
    }
  ],
  
  workoutSets: [
    {
      _id: "set-001",
      exerciseId: 501,
      exerciseName: "Bench Press",
      setNumber: 1,
      setType: "WORKING", // WARMUP, WORKING, DROP, FAILURE
      reps: 10,
      weight: 60.0,
      volume: 600.0,
      rpe: 7.5,
      timestamp: ISODate("2025-12-22T10:05:00Z"),
      personalRecords: [
        {
          recordType: "MAX_WEIGHT",
          value: 60.0,
          improvement: 5.0
        }
      ]
    }
  ],
  
  metrics: {
    totalSets: 22,
    totalReps: 230,
    totalVolume: 4250.5,
    uniqueExercises: 6,
    personalRecordsAchieved: 2,
    caloriesBurned: 320
  },
  
  workedMuscleGroups: ["CHEST", "SHOULDERS", "TRICEPS"],
  
  createdAt: ISODate("2025-12-22T10:00:00Z"),
  updatedAt: ISODate("2025-12-22T10:45:00Z")
}
```

#### personal_records
```javascript
{
  _id: ObjectId("..."),
  userId: 3602951135283784,
  exerciseId: 501,
  exerciseName: "Bench Press",
  recordType: "MAX_WEIGHT", // MAX_WEIGHT, MAX_REPS, MAX_VOLUME, ESTIMATED_1RM
  value: 60.0,
  reps: 10,
  weight: 60.0,
  workoutId: "workout-xyz789",
  workoutSetId: "set-001",
  achievedAt: ISODate("2025-12-22T10:05:00Z"),
  previousRecord: 55.0,
  improvement: 5.0,
  
  createdAt: ISODate("2025-12-22T10:05:00Z")
}
```

---

## API Reference

### Quick Reference Table

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/routine-folders/user` | GET | Get user's folders |
| `/api/routine-folders` | POST | Create folder |
| `/api/routine-folders/{id}` | PUT | Update folder |
| `/api/routine-folders/{id}` | DELETE | Delete folder |
| `/api/routines/folder/{folderId}` | GET | Get routines in folder |
| `/api/routines/{id}` | GET | Get routine details |
| `/api/routines` | POST | Create routine |
| `/api/routines/{id}` | PUT | Update routine |
| `/api/routines/{id}` | DELETE | Delete routine |
| `/api/routines/{id}/exercises` | POST | Add exercise |
| `/api/routines/{id}/exercises/{exerciseId}` | PUT | Update exercise |
| `/api/routines/{id}/exercises/{exerciseId}` | DELETE | Remove exercise |
| `/api/workouts/start` | POST | Start workout |
| `/api/workouts/{id}/complete` | POST | Complete workout |
| `/api/workouts/{id}/sets` | POST | Log set |
| `/api/workouts/history` | GET | Get workout history |
| `/api/personal-records` | GET | Get all PRs |
| `/api/personal-records/exercise/{id}` | GET | Get PRs for exercise |

---

## Timeline Summary
```
T+0s:     User opens app → Views routine folders
T+1s:     Taps "Upper Body" folder
T+2s:     Views "Push Day" routine
T+3s:     Taps "Start Workout"
T+4s:     Workout session created in MongoDB
T+4s:     Active workout screen loads

[User performs workout]

T+5m:     Logs Set 1: Bench Press 60kg x 10
T+5m:     PR detected: MAX_WEIGHT +5kg
T+5m:     PR celebration modal shown
T+7m:     Rest timer completes
T+7m:     Logs Set 2: Bench Press 60kg x 10

[... continues for 45 minutes ...]

T+45m:    User taps "End Workout"
T+45m:    Workout marked COMPLETED in MongoDB
T+45m:    WorkoutCompletedEvent published to Kafka
T+45m:    Workout summary shown to user

[Gamification processing - see Challenge System Journey]

T+45m+1s: Gamification service consumes event
T+45m+1s: Streaks updated, XP awarded, coins awarded
T+45m+1s: Challenge progress updated
T+45m+1s: Badge earned notification
T+45m+30s: Frontend polls /api/challenges/active
T+45m+30s: Challenge completion detected
T+45m+30s: 🎉 Challenge celebration modal shown
```

---

## Key Design Principles

### 1. Hybrid Database Strategy
- **MySQL**: Structured, relational data (routines, folders, exercises)
- **MongoDB**: Document data with nested structures (workouts, sets, PRs)

### 2. Real-Time Feedback
- Immediate PR detection during set logging
- Instant celebration modals
- Live progress tracking

### 3. Offline-First (Future)
- Local storage for active workouts
- Sync when online
- Conflict resolution

### 4. Performance Optimization
- Denormalized exercise data in planned_exercises
- Embedded documents in workout_sessions
- Indexed queries for fast lookups

### 5. Data Integrity
- Foreign key constraints in MySQL
- Validation at service layer
- Transaction boundaries for critical operations

---

## Common Flows

### Beginner User Journey
```
Day 1:
→ Creates first routine "Full Body Workout"
→ Adds 5 exercises
→ Starts first workout
→ Logs all sets
→ Achieves 5 personal records (first time doing exercises)
→ Completes workout
→ Earns "First Workout" badge
→ Gains 72 XP, levels up to Level 2
→ Earns 31 fitness coins

Day 2:
→ Views workout history
→ Sees yesterday's workout
→ Starts same routine again
→ Beats 2 personal records
→ Completes daily challenge "Rep Master"
→ Gains additional 50 XP from challenge
```

### Advanced User Journey
```
→ Has 10+ routines across 4 folders
→ Tracks progressive overload weekly
→ Maintains 30-day workout streak
→ Has 50+ personal records
→ Completes weekly challenges consistently
→ Level 25+ with multiple badges
→ Uses fitness coins to buy streak freeze
```

---

## Troubleshooting

### Workout Not Saving

**Symptoms:** Sets logged but not appearing in database

**Checks:**
1. Workout status is IN_PROGRESS
2. MongoDB connection healthy
3. Check service logs for exceptions
4. Verify user ownership of workout

### PR Not Detecting

**Symptoms:** User beats previous PR but not recognized

**Checks:**
1. Previous PR exists in database?
2. Current set values correct?
3. Calculation logic accurate?
4. Check PersonalRecordService logs

### Routine Not Loading

**Symptoms:** Routine detail screen blank

**Checks:**
1. Routine exists in MySQL
2. User owns routine
3. Planned exercises loaded
4. Check foreign key constraints

---

## Performance Considerations

### Database Indexes
```sql
-- MySQL
CREATE INDEX idx_user_routines ON routines(user_id, created_at DESC);
CREATE INDEX idx_folder_routines ON routines(folder_id, order_index);
CREATE INDEX idx_routine_exercises ON planned_exercises(routine_id, order_index);

-- MongoDB
db.workout_sessions.createIndex({ userId: 1, startTime: -1 });
db.workout_sessions.createIndex({ status: 1 });
db.personal_records.createIndex({ userId: 1, exerciseId: 1, recordType: 1 });
```

### Caching Strategy
```
Redis Cache (Future):
- Exercise library: 1 hour TTL
- User routines: 5 min TTL
- Active workout: No cache (real-time)
```

---

## Future Enhancements

1. **Advanced Analytics**
    - Volume charts over time
    - Muscle group distribution
    - Progressive overload tracking
    - Training frequency analysis

2. **Social Features**
    - Share workouts
    - Follow friends
    - Workout challenges with friends

3. **AI Coaching**
    - Suggest next exercise
    - Recommend rest time
    - Auto-adjust weights
    - Form analysis (using camera)

4. **Offline Mode**
    - Full offline workout tracking
    - Background sync
    - Conflict resolution

5. **Wearable Integration**
    - Apple Watch support
    - Heart rate monitoring
    - Auto-detect sets

---

## Conclusion

The workout system provides:
- ✅ Flexible routine organization
- ✅ Real-time workout tracking
- ✅ Automatic PR detection
- ✅ Comprehensive history
- ✅ Seamless gamification integration

**Status:** Production Ready 🚀

---

*Last Updated: December 22, 2025*