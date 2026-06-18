package com.linkvault.resources.dto;

import com.linkvault.resources.enums.ResourceType;
import com.linkvault.tags.dto.TagResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResourceResponse(
    UUID id,
    UUID vaultId,
    String vaultName,
    UUID folderId,
    String folderName,
    String title,
    String description,
    ResourceType resourceType,
    String url,
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    String storageProvider,
    String storageKey,
    String content,
    String codeLanguage,
    String sourceName,
    String thumbnailUrl,
    String previewTitle,
    String previewDescription,
    String faviconUrl,
    String siteName,
    String canonicalUrl,
    Instant previewFetchedAt,
    String previewStatus,
    String previewError,
    Boolean isFavorite,
    Boolean isArchived,
    List<TagResponse> tags,
    com.linkvault.common.enums.PublicAccess publicAccess,
    Instant createdAt,
    Instant updatedAt
) {
}