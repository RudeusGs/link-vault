package com.linkvault.storage;

public record StorageResult(
    String url,
    String secureUrl,
    String publicId,
    String originalFilename,
    long size,
    String mimeType
) {
}
