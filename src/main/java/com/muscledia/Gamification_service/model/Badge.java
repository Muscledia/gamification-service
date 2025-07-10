package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.BadgeType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "badges")
public class Badge {
    @Id
    private String badgeId;

    @Indexed(unique = true) // Ensure badge names are unique
    private String name;

    private String description;

    private BadgeType badgeType; // Enum: STREAK, PR, EXERCISE, SEASONAL, CHAMPION

    private String imageUrl;

    private int pointsAwarded;

    // Flexible criteria for earning the badge (e.g., {"type": "WORKOUT_COUNT",
    // "targetValue": 5, "period": "WEEKLY"})
    private Map<String, Object> criteria;

    private Instant createdAt;
}
