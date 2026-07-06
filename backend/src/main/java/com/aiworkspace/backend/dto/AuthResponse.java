package com.aiworkspace.backend.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
