package com.muscledia.Gamification_service.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreakUpdateRequest {

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotBlank(message = "Streak type cannot be blank")
    private String streakType;

    @NotNull(message = "Streak continues flag cannot be null")
    private Boolean streakContinues;
}