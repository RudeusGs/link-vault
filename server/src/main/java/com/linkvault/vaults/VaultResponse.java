package com.linkvault.vaults;

import java.time.Instant;
import java.util.UUID;

public record VaultResponse(
    UUID id,
    String name,
    String description,
    String icon,
    String color,
    Instant createdAt,
    Instant updatedAt
) {
}
