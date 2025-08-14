package com.muscledia.Gamification_service.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when a new user registers in the system
 * Triggers automatic gamification profile creation
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Username is required")
    private String username;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Registration date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant registrationDate;

    private Map<String, Object> userPreferences;
    private String goalType;
    private String initialAvatarType;
    private String eventType = "USER_REGISTERED";

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    /**
     * Validate event content
     */
    public boolean isValid() {
        return userId != null &&
                username != null && !username.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                registrationDate != null;
    }
}
