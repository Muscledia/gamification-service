package com.muscledia.Gamification_service.repository;

import com.muscledia.Gamification_service.model.StoreItem;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface StoreItemRepository extends ReactiveMongoRepository<StoreItem, String> {
    Flux<StoreItem> findByIsActiveTrue();
    Flux<StoreItem> findByCategory(String category);
}