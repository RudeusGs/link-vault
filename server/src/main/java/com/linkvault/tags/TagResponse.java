package com.linkvault.tags;

import java.util.UUID;

public record TagResponse(
    UUID id,
    String name,
    String color,
    long usageCount
) {
}
