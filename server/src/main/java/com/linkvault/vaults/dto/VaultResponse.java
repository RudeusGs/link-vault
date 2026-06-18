package com.linkvault.vaults.dto;

import java.time.Instant;
import java.util.UUID;

public record VaultResponse(
    UUID id,
    String name,
    String description,
    String icon,
    String color,
    com.linkvault.common.enums.PublicAccess publicAccess,
    Instant createdAt,
    Instant updatedAt
) {
}