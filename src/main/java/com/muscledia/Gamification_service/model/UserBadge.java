package com.muscledia.Gamification_service.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge {
    private String badgeId;

    @Builder.Default
    private Instant earnedAt = Instant.now();

    // ADDED: Optional fields for additional badge information
    private String badgeName;        // FIXED: Added this field
    private String description;      // FIXED: Added this field
    private String category;         // FIXED: Added this field
    private Integer pointsAwarded;   // FIXED: Added this field
}
