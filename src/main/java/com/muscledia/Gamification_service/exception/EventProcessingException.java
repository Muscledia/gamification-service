package com.muscledia.Gamification_service.exception;

public class EventProcessingException extends RuntimeException {
    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
