package com.muscledia.Gamification_service.exception;

import com.muscledia.Gamification_service.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        /**
         * Handle validation errors for request body
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
                        MethodArgumentNotValidException ex) {

                log.warn("Validation error occurred", ex);

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Validation failed", errors));
        }

        /**
         * Handle constraint violations (path variables, request parameters)
         */
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<String>> handleConstraintViolationException(
                        ConstraintViolationException ex) {

                log.warn("Constraint violation occurred", ex);

                String message = ex.getConstraintViolations().iterator().next().getMessage();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Invalid parameter: " + message));
        }

        /**
         * Handle type mismatch errors (wrong parameter types)
         */
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<String>> handleTypeMismatchException(
                        MethodArgumentTypeMismatchException ex) {

                log.warn("Type mismatch error occurred", ex);

                String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(message));
        }

        /**
         * Handle illegal argument exceptions
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(
                        IllegalArgumentException ex) {

                log.warn("Illegal argument exception occurred", ex);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Handle resource not found exceptions
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<String>> handleResourceNotFoundException(
                        ResourceNotFoundException ex) {

                log.warn("Resource not found", ex);

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Handle business logic exceptions
         */
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<String>> handleBusinessException(
                        BusinessException ex) {

                log.warn("Business exception occurred", ex);

                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Handle duplicate key exceptions (MongoDB)
         */
        @ExceptionHandler(DuplicateKeyException.class)
        public ResponseEntity<ApiResponse<String>> handleDuplicateKeyException(
                        DuplicateKeyException ex) {

                log.warn("Duplicate key exception occurred", ex);

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error("Resource already exists"));
        }

        /**
         * Handle data integrity violations
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(
                        DataIntegrityViolationException ex) {

                log.warn("Data integrity violation occurred", ex);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Data integrity violation"));
        }

        /**
         * Handle unauthorized access
         */
        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiResponse<String>> handleUnauthorizedException(
                        UnauthorizedException ex) {

                log.warn("Unauthorized access attempt", ex);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Handle forbidden access
         */
        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiResponse<String>> handleForbiddenException(
                        ForbiddenException ex) {

                log.warn("Forbidden access attempt", ex);

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Handle authentication errors
         */
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiResponse<String>> handleAuthenticationException(
                        AuthenticationException ex) {

                log.warn("Authentication error occurred", ex);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Handle security exceptions (access denied)
         */
        @ExceptionHandler(SecurityException.class)
        public ResponseEntity<ApiResponse<String>> handleSecurityException(
                        SecurityException ex) {

                log.warn("Security exception occurred", ex);

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Handle all other unexpected exceptions
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {

                log.error("Unexpected error occurred", ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
        }

        /**
         * Handle runtime exceptions
         */
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {

                log.error("Runtime exception occurred", ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("A runtime error occurred. Please try again later."));
        }
}