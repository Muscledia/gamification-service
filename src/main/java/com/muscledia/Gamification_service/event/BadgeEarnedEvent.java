package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Event published when a user earns a badge.
 * This is an OUTBOUND event that notifies other services of achievements.
 * 
 * Uses SuperBuilder pattern for immutable-like construction while
 * maintaining compatibility with the abstract BaseEvent class.
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class BadgeEarnedEvent extends BaseEvent {
    /**
     * Unique identifier of the badge earned
     */
    @NotBlank
    private String badgeId;

    /**
     * Name of the badge for display purposes
     */
    @NotBlank
    private String badgeName;

    /**
     * Type/category of the badge
     */
    @NotBlank
    private String badgeType;

    /**
     * Description of what the badge represents
     */
    private String badgeDescription;

    /**
     * Points awarded for earning this badge
     */
    @Min(0)
    private Integer pointsAwarded;

    /**
     * Rarity level of the badge
     */
    private String rarity; // "COMMON", "RARE", "EPIC", "LEGENDARY"

    /**
     * When the badge was earned
     */
    @NotNull
    private Instant earnedAt;

    /**
     * Activity or achievement that triggered this badge
     */
    private String triggeringActivity;

    /**
     * Reference to the event that caused this badge to be earned
     */
    private String triggeringEventId;

    /**
     * User's total badge count after earning this badge
     */
    @Min(0)
    private Integer totalBadgeCount;

    /**
     * User's new level (if level up occurred)
     */
    private Integer newUserLevel;

    /**
     * User's new total points after earning this badge
     */
    @Min(0)
    private Integer newTotalPoints;

    /**
     * Additional metadata about the achievement
     */
    private Map<String, Object> achievementData;

    /**
     * Default constructor for Jackson and Lombok
     */
    public BadgeEarnedEvent() {
        super();
    }

    /**
     * JSON constructor for Jackson deserialization
     */
    @JsonCreator
    public BadgeEarnedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("userId") Long userId,
            @JsonProperty("source") String source,
            @JsonProperty("version") String version,
            @JsonProperty("badgeId") String badgeId,
            @JsonProperty("badgeName") String badgeName,
            @JsonProperty("badgeType") String badgeType,
            @JsonProperty("badgeDescription") String badgeDescription,
            @JsonProperty("pointsAwarded") Integer pointsAwarded,
            @JsonProperty("rarity") String rarity,
            @JsonProperty("earnedAt") Instant earnedAt,
            @JsonProperty("triggeringActivity") String triggeringActivity,
            @JsonProperty("triggeringEventId") String triggeringEventId,
            @JsonProperty("totalBadgeCount") Integer totalBadgeCount,
            @JsonProperty("newUserLevel") Integer newUserLevel,
            @JsonProperty("newTotalPoints") Integer newTotalPoints,
            @JsonProperty("achievementData") Map<String, Object> achievementData) {

        super(eventId, timestamp, userId, source, version);  // FIXED: Correct order!
        this.badgeId = badgeId;
        this.badgeName = badgeName;
        this.badgeType = badgeType;
        this.badgeDescription = badgeDescription;
        this.pointsAwarded = pointsAwarded;
        this.rarity = rarity;
        this.earnedAt = earnedAt;
        this.triggeringActivity = triggeringActivity;
        this.triggeringEventId = triggeringEventId;
        this.totalBadgeCount = totalBadgeCount;
        this.newUserLevel = newUserLevel;
        this.newTotalPoints = newTotalPoints;
        this.achievementData = achievementData;
    }

    @Override
    public String getEventType() {
        return "BADGE_EARNED";
    }

    @Override
    public boolean isValid() {
        return badgeId != null && !badgeId.trim().isEmpty()
                && badgeName != null && !badgeName.trim().isEmpty()
                && badgeType != null && !badgeType.trim().isEmpty()
                && pointsAwarded != null && pointsAwarded >= 0
                && earnedAt != null
                && totalBadgeCount != null && totalBadgeCount >= 0
                && newTotalPoints != null && newTotalPoints >= 0;
    }

    @Override
    public BaseEvent withNewTimestamp() {
        return this.toBuilder()
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public double getIntensityScore() {
        return 0;
    }

    @Override
    public boolean isStreakEligible() {
        return false;
    }

    /**
     * Check if this badge earning also resulted in a level up
     */
    public boolean causedLevelUp() {
        return newUserLevel != null;
    }

    /**
     * Get the significance of this badge achievement
     */
    public String getAchievementSignificance() {
        if ("LEGENDARY".equalsIgnoreCase(rarity))
            return "EXCEPTIONAL";
        if ("EPIC".equalsIgnoreCase(rarity))
            return "MAJOR";
        if ("RARE".equalsIgnoreCase(rarity))
            return "SIGNIFICANT";
        if (pointsAwarded != null && pointsAwarded >= 500)
            return "HIGH_VALUE";
        return "STANDARD";
    }

    /**
     * Check if this is a milestone badge (every 10th, 25th, etc.)
     */
    public boolean isMilestoneBadge() {
        return totalBadgeCount != null && (totalBadgeCount % 25 == 0 ||
                totalBadgeCount % 10 == 0 ||
                totalBadgeCount == 5 ||
                totalBadgeCount == 1 // First badge is special
        );
    }

    /**
     * Create a user-friendly achievement message
     */
    public String getAchievementMessage() {
        StringBuilder message = new StringBuilder();
        message.append("🎉 ").append(badgeName).append(" badge earned!");

        if (pointsAwarded != null && pointsAwarded > 0) {
            message.append(" (+").append(pointsAwarded).append(" points)");
        }

        if (causedLevelUp()) {
            message.append(" Level up to ").append(newUserLevel).append("!");
        }

        return message.toString();
    }

    /**
     * Check if badge is rare (RARE, EPIC, or LEGENDARY)
     */
    public boolean isRareBadge() {
        if (rarity == null) return false;

        String rarityUpper = rarity.trim().toUpperCase();
        return rarityUpper.equals("RARE") ||
                rarityUpper.equals("EPIC") ||
                rarityUpper.equals("LEGENDARY");
    }
}