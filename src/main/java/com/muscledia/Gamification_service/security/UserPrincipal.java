package com.muscledia.Gamification_service.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long userId;
    private String username;
    private List<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // We don't store passwords in JWT tokens
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // JWT expiration is handled separately
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Account locking is handled by user service
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // JWT expiration is handled separately
    }

    @Override
    public boolean isEnabled() {
        return true; // User activation is handled by user service
    }

    /**
     * Get user ID as Long
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.toUpperCase()));
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}