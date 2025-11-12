package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.UserJourneyProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserJourneyProfileRepository extends MongoRepository<UserJourneyProfile, String> {
    Optional<UserJourneyProfile> findByUserId(Long userId); // Return Optional

    // Additional useful queries
    List<UserJourneyProfile> findByCurrentPhase(String phase);
    List<UserJourneyProfile> findByCurrentLevelGreaterThan(int level);

    @Query("{'activeJourneyTags': {'$in': ?0}}")
    List<UserJourneyProfile> findByJourneyTagsIn(List<String> tags);
}
