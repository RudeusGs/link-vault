package com.linkvault.storage;

public record StorageFile(
    byte[] content,
    String mimeType
) {
}
