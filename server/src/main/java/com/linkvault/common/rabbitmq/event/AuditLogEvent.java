package com.linkvault.common.rabbitmq.event;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEvent(
    UUID workspaceId,
    UUID actorUserId,
    String action,
    String targetType,
    UUID targetId,
    String metadata,
    Instant occurredAt
) {
}
