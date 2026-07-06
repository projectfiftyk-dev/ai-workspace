package com.aiworkspace.backend.dto;

import com.aiworkspace.backend.model.User;

import java.time.Instant;

public record UserResponse(
        String id,
        String email,
        String name,
        String orgId,
        String role,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getOrgId(), user.getRole(), user.getCreatedAt());
    }
}
