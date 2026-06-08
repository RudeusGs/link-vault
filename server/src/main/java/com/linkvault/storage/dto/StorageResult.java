package com.linkvault.storage.dto;

public record StorageResult(
    String url,
    String secureUrl,
    String publicId,
    String originalFilename,
    long size,
    String mimeType
) {
}
