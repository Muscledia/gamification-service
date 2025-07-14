package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
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

    // Type of criteria for earning the badge (provides type safety and validation)
    private BadgeCriteriaType criteriaType;

    // Flexible criteria parameters for earning the badge
    // (e.g., {"targetValue": 5, "period": "WEEKLY", "exerciseId": "123"})
    private Map<String, Object> criteriaParams;

    private Instant createdAt;
}
