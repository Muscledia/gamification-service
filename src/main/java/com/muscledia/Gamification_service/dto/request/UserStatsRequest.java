package com.muscledia.Gamification_service.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsRequest {

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "User stats cannot be null")
    private Map<String, Object> userStats;
}