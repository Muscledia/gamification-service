package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;

@Slf4j
public class InstantStringDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            String timestampStr = p.getValueAsString();
            if (timestampStr != null && !timestampStr.trim().isEmpty()) {
                return Instant.parse(timestampStr);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to parse ISO timestamp: {}", e.getMessage());
            return null;
        }
    }
}
