package com.linkvault.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSessionResponse(
    UUID id,
    Instant createdAt,
    Instant expiresAt,
    Instant revokedAt,
    Instant lastUsedAt,
    String userAgent,
    String ipAddress
) {
}