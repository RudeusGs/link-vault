package com.linkvault.resources.dto;

import com.linkvault.resources.enums.ResourceType;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.linkvault.tags.dto.TagResponse;

public record PublicResourceResponse(
    UUID id,
    UUID vaultId,
    UUID folderId,
    String title,
    String description,
    ResourceType resourceType,
    String url,
    String content,
    String codeLanguage,
    String sourceName,
    String siteName,
    String thumbnailUrl,
    String faviconUrl,
    Long fileSize,
    String mimeType,
    String previewTitle,
    String previewDescription,
    Instant createdAt,
    Instant updatedAt,
    List<TagResponse> tags
) {}
