package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.time.Instant;

@Slf4j
public class  TimestampDeserializer extends JsonDeserializer<Instant> {


    @Override
    public Instant deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException, JacksonException {
        try {
            // Handle decimal timestamp like: 1755892055.689000000
            double timestamp = p.getDoubleValue();
            long seconds = (long) timestamp;
            int nanos = (int) ((timestamp - seconds) * 1_000_000_000);
            return Instant.ofEpochSecond(seconds, nanos);
        } catch (Exception e) {
            log.warn("Failed to parse decimal timestamp, trying string format: {}", e.getMessage());
            try {
                // Fallback: try to parse as ISO string
                String timestampStr = p.getValueAsString();
                if (timestampStr != null && !timestampStr.trim().isEmpty()) {
                    return Instant.parse(timestampStr);
                }
            } catch (Exception ex) {
                log.error("Failed to parse timestamp in any format: {}", ex.getMessage());
            }
            return null;
        }
    }
}
