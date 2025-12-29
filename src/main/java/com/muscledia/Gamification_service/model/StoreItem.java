package com.muscledia.Gamification_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "store_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreItem {
    @Id
    private String id;

    private String name;
    private String description;
    private String category;  // UTILITY, COSMETIC, BOOSTER
    private Integer price;  // In fitness coins

    private String iconUrl;
    private String rarity;  // COMMON, RARE, EPIC, LEGENDARY

    private Boolean isLimitedEdition;
    private LocalDateTime availableUntil;

    private Integer maxPurchases;
    private Boolean isActive;

    private ItemEffect effect;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemEffect {
        private String type;  // STREAK_FREEZE, CHALLENGE_SKIP, COIN_MULTIPLIER, XP_BOOST
        private Integer duration;  // Hours
        private Double multiplier;
    }
}