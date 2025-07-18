package com.muscledia.Gamification_service.exception;

/**
 * Exception thrown when business logic rules are violated
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}