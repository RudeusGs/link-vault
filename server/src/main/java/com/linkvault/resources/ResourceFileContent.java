package com.linkvault.resources;

import java.util.UUID;

public record ResourceFileContent(
    UUID id,
    String fileName,
    String mimeType,
    byte[] content
) {
}
