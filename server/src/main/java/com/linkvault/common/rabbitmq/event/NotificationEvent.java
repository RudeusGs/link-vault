package com.linkvault.common.rabbitmq.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
    UUID workspaceId,
    UUID actorUserId,
    String eventType,
    String title,
    String message,
    UUID targetId,
    Instant occurredAt
) {
}
