package com.muscledia.Gamification_service.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserRegisteredEvent extends BaseEvent {

    @NotBlank(message = "Username is required")
    private String username;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Registration date is required")
    private Instant registrationDate;

    private Map<String, Object> userPreferences;
    private String goalType;
    private String initialAvatarType;


    @Override
    public String getEventType() {
        return "USER_REGISTERED";
    }

    @Override
    public boolean isValid() {
        return username != null && !username.trim().isEmpty()
                && email != null && !email.trim().isEmpty()
                && registrationDate != null;
    }

    @Override
    public BaseEvent withNewTimestamp() {
        return this.toBuilder()
                .timestamp(Instant.now())
                .build();
    }
}
