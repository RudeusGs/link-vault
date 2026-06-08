package com.linkvault.resources.dto;

import java.util.UUID;

public record ResourceFileContent(
    UUID id,
    String fileName,
    String mimeType,
    byte[] content
) {
}