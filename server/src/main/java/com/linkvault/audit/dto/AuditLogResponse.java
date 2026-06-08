package com.linkvault.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID workspaceId,
    UUID actorUserId,
    String actorUsername,
    String action,
    String targetType,
    UUID targetId,
    String metadata,
    Instant createdAt
) {
}