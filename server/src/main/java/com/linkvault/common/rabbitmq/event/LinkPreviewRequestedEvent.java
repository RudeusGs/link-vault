package com.linkvault.common.rabbitmq.event;

import java.time.Instant;
import java.util.UUID;

public record LinkPreviewRequestedEvent(
    UUID resourceId,
    UUID workspaceId,
    UUID vaultId,
    UUID folderId,
    String url,
    boolean force,
    Instant requestedAt
) {
}
