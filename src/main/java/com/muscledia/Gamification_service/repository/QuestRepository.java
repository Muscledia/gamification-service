package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.Quest;
import com.muscledia.Gamification_service.model.enums.ObjectiveType;
import com.muscledia.Gamification_service.model.enums.QuestType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestRepository extends MongoRepository<Quest, String> {

    /**
     * Find quest by name
     */
    Optional<Quest> findByName(String name);

    /**
     * Find all quests of a specific type
     */
    List<Quest> findByQuestType(QuestType questType);

    /**
     * Find quests by objective type
     */
    List<Quest> findByObjectiveType(ObjectiveType objectiveType);

    /**
     * Find active quests (current time between start and end date)
     */
    @Query("{ 'startDate' : { $lte: ?0 }, 'endDate' : { $gte: ?0 } }")
    List<Quest> findActiveQuests(Instant currentTime);

    /**
     * Find quests suitable for user level
     */
    List<Quest> findByRequiredLevelLessThanEqual(int userLevel);

    /**
     * Find active quests for user level
     */
    @Query("{ 'startDate' : { $lte: ?0 }, 'endDate' : { $gte: ?0 }, 'requiredLevel' : { $lte: ?1 } }")
    List<Quest> findActiveQuestsForUserLevel(Instant currentTime, int userLevel);

    /**
     * Find repeatable quests
     */
    List<Quest> findByRepeatableTrue();

    /**
     * Find quests by exercise ID
     */
    List<Quest> findByExerciseId(String exerciseId);

    /**
     * Find quests by muscle group ID
     */
    List<Quest> findByMuscleGroupId(String muscleGroupId);

    /**
     * Find quests by type and level requirement
     */
    List<Quest> findByQuestTypeAndRequiredLevelLessThanEqual(QuestType questType, int userLevel);

    /**
     * Find upcoming quests (start date in the future)
     */
    @Query("{ 'startDate' : { $gt: ?0 } }")
    List<Quest> findUpcomingQuests(Instant currentTime);

    /**
     * Find expired quests (end date in the past)
     */
    @Query("{ 'endDate' : { $lt: ?0 } }")
    List<Quest> findExpiredQuests(Instant currentTime);

    /**
     * Find quests with minimum reward threshold
     */
    @Query("{ $or: [ { 'expReward' : { $gte: ?0 } }, { 'pointsReward' : { $gte: ?1 } } ] }")
    List<Quest> findQuestsWithMinimumRewards(int minExp, int minPoints);

    /**
     * Find quests ordered by creation date (newest first)
     */
    List<Quest> findAllByOrderByCreatedAtDesc();

    /**
     * Find quests created after a specific date (for analysis)
     */
    @Query("{ 'createdAt' : { $gte: ?0 } }")
    List<Quest> findQuestsCreatedAfter(Instant date);

    /**
     * Find daily/weekly quests for scheduling
     */
    @Query("{ 'questType' : { $in: ['DAILY', 'WEEKLY'] } }")
    List<Quest> findScheduledQuests();
}