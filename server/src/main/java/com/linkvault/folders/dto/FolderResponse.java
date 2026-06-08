package com.linkvault.folders.dto;

import java.time.Instant;
import java.util.UUID;

public record FolderResponse(
    UUID id,
    UUID vaultId,
    UUID parentId,
    String name,
    String description,
    String icon,
    Integer sortOrder,
    Instant createdAt,
    Instant updatedAt
){}