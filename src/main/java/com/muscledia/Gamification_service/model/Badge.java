package com.muscledia.Gamification_service.model;

import com.muscledia.Gamification_service.model.enums.BadgeType;
import com.muscledia.Gamification_service.model.enums.BadgeCriteriaType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "badges")
public class Badge {
    @Id
    private String badgeId;

    @Indexed(unique = true) // Ensure badge names are unique
    @NotBlank(message = "Badge name is required")
    @Size(min = 3, max = 100, message = "Badge name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Badge description is required")
    @Size(max = 500, message = "Badge description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Badge type is required")
    private BadgeType badgeType; // Enum: STREAK, PR, EXERCISE, SEASONAL, CHAMPION

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String imageUrl;

    @Min(value = 0, message = "Points awarded must be non-negative")
    @Max(value = 10000, message = "Points awarded must not exceed 10,000")
    private int pointsAwarded;

    // Type of criteria for earning the badge (provides type safety and validation)
    @NotNull(message = "Badge criteria type is required")
    private BadgeCriteriaType criteriaType;

    // Flexible criteria parameters for earning the badge
    // (e.g., {"targetValue": 5, "period": "WEEKLY", "exerciseId": "123"})
    @NotNull(message = "Criteria parameters are required")
    @Size(min = 1, message = "At least one criteria parameter is required")
    private Map<String, Object> criteriaParams;

    private Instant createdAt;
}
