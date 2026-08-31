package com.finapse.dto;

import com.finapse.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public view of a user. Deliberately excludes the password hash and any
 * security bookkeeping fields.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
