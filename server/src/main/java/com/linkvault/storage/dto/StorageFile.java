package com.linkvault.storage.dto;

public record StorageFile(
    byte[] content,
    String mimeType
) {
}
