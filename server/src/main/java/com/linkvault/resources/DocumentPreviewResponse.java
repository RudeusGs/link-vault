package com.linkvault.resources;

import java.util.UUID;

public record DocumentPreviewResponse(
    UUID id,
    String title,
    String fileName,
    String mimeType,
    String plainText,
    int paragraphCount,
    boolean supported,
    String reason
) {
}
