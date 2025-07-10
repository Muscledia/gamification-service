package com.muscledia.Gamification_service.model;


import com.muscledia.Gamification_service.model.enums.QuestStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class UserQuestProgress {
    private String questId;

    private int objectiveProgress;

    private QuestStatus status;

    private Instant startDate;

    private Instant completionDate;

    private Instant createdAt;

}
