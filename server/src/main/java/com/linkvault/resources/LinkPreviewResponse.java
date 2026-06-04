package com.linkvault.resources;

import java.time.Instant;

public record LinkPreviewResponse(
    String requestedUrl,
    String url,
    String sourceName,
    String domain,
    String thumbnailUrl,
    String previewTitle,
    String previewDescription,
    String faviconUrl,
    String siteName,
    String canonicalUrl,
    Instant previewFetchedAt,
    String previewStatus,
    String previewError
) {
    public boolean successful() {
        return "OK".equals(previewStatus);
    }
}
