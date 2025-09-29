package com.muscledia.Gamification_service.exception;

/**
 * Exception thrown when a user has valid credentials but lacks permission for
 * the requested resource
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}