package com.linkvault.resources;

import java.util.UUID;

public record ResourcePreviewResponse(
    UUID id,
    ResourceType resourceType,
    String title,
    String previewUrl,
    String mimeType,
    String fileName,
    boolean supported,
    String reason
) {
}
