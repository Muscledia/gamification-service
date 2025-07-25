package com.muscledia.Gamification_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.Duration;

/**
 * Event published when a user completes a quest.
 * This is an OUTBOUND event for quest completion notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuestCompletedEvent extends BaseEvent {

    @NotBlank
    private String questId;

    @NotBlank
    private String questName;

    @NotBlank
    private String questType;

    @Min(0)
    private Integer pointsRewarded;

    @Min(0)
    private Integer expRewarded;

    @NotNull
    private Instant completedAt;

    @NotNull
    private Instant questStartedAt;

    private String difficulty;

    @Override
    public String getEventType() {
        return "QUEST_COMPLETED";
    }

    @Override
    public boolean isValid() {
        return questId != null && !questId.trim().isEmpty()
                && questName != null && !questName.trim().isEmpty()
                && questType != null && !questType.trim().isEmpty()
                && pointsRewarded != null && pointsRewarded >= 0
                && completedAt != null
                && questStartedAt != null
                && completedAt.isAfter(questStartedAt);
    }

    public Duration getCompletionTime() {
        return Duration.between(questStartedAt, completedAt);
    }

    public boolean isRapidCompletion() {
        Duration completionTime = getCompletionTime();
        return switch (questType.toUpperCase()) {
            case "DAILY" -> completionTime.toHours() <= 6;
            case "WEEKLY" -> completionTime.toDays() <= 2;
            default -> false;
        };
    }
}