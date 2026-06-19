package com.linkvault.common.rabbitmq.event;

import java.time.Instant;
import java.util.UUID;

public record CleanupEvent(
    String cleanupType,
    UUID workspaceId,
    UUID targetId,
    Instant requestedAt
) {
}
