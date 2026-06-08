package com.linkvault.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String displayName,
    String avatarUrl,
    Boolean isVerified,
    Instant createdAt,
    Instant updatedAt
) {
}