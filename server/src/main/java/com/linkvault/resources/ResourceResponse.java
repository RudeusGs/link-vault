package com.linkvault.resources;

import com.linkvault.tags.TagResponse;
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
    Boolean isFavorite,
    Boolean isArchived,
    List<TagResponse> tags,
    Instant createdAt,
    Instant updatedAt
) {
}
