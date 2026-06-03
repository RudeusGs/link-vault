package com.linkvault.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 1000) String description,
    @NotNull ResourceType resourceType,
    @Size(max = 2000) String url,
    String content,
    @Size(max = 80) String codeLanguage,
    @Size(max = 255) String sourceName,
    @Size(max = 2000) String thumbnailUrl
) {
}
