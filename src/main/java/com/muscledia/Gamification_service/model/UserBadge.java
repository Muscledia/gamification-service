package com.muscledia.Gamification_service.model;


import lombok.Data;

import java.time.Instant;

@Data
public class UserBadge {
    private String badgeId;
    private Instant earnedAt;
}
