package com.linkvault.resources.mapper;

import com.linkvault.resources.dto.ResourceResponse;
import com.linkvault.resources.entity.Resource;
import com.linkvault.resources.entity.ResourceTag;
import com.linkvault.resources.repository.ResourceTagRepository;
import com.linkvault.tags.dto.TagResponse;
import com.linkvault.tags.entity.Tag;
import com.linkvault.tags.service.TagService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import com.linkvault.common.pagination.PageResponse;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    private final ResourceTagRepository resourceTagRepository;
    private final TagService tagService;

    public ResourceMapper(ResourceTagRepository resourceTagRepository, TagService tagService) {
        this.resourceTagRepository = resourceTagRepository;
        this.tagService = tagService;
    }

    public ResourceResponse toResponse(Resource resource) {
        return toResponse(resource, tagsByResourceId(List.of(resource)));
    }

    public List<ResourceResponse> toResponses(List<Resource> resources) {
        Map<UUID, List<TagResponse>> tagsByResourceId = tagsByResourceId(resources);
        return resources.stream()
            .map(resource -> toResponse(resource, tagsByResourceId))
            .toList();
    }

    public PageResponse<ResourceResponse> toPageResponse(Page<Resource> page) {
        Map<UUID, List<TagResponse>> tagsByResourceId = tagsByResourceId(page.getContent());
        Page<ResourceResponse> responsePage = page.map(resource -> toResponse(resource, tagsByResourceId));
        return PageResponse.from(responsePage);
    }

    private ResourceResponse toResponse(Resource resource, Map<UUID, List<TagResponse>> tagsByResourceId) {
        return new ResourceResponse(
            resource.getId(),
            resource.getVault().getId(),
            resource.getVault().getName(),
            resource.getFolder() == null ? null : resource.getFolder().getId(),
            resource.getFolder() == null ? null : resource.getFolder().getName(),
            resource.getTitle(),
            resource.getDescription(),
            resource.getResourceType(),
            resource.getUrl(),
            resource.getFileUrl(),
            resource.getFileName(),
            resource.getFileSize(),
            resource.getMimeType(),
            resource.getStorageProvider(),
            resource.getStorageKey(),
            resource.getContent(),
            resource.getCodeLanguage(),
            resource.getSourceName(),
            resource.getThumbnailUrl(),
            resource.getPreviewTitle(),
            resource.getPreviewDescription(),
            resource.getFaviconUrl(),
            resource.getSiteName(),
            resource.getCanonicalUrl(),
            resource.getPreviewFetchedAt(),
            resource.getPreviewStatus(),
            resource.getPreviewError(),
            resource.getIsFavorite(),
            resource.getIsArchived(),
            tagsByResourceId.getOrDefault(resource.getId(), List.of()),
            resource.getCreatedAt(),
            resource.getUpdatedAt()
        );
    }

    private Map<UUID, List<TagResponse>> tagsByResourceId(List<Resource> resources) {
        List<UUID> resourceIds = resources.stream()
            .map(Resource::getId)
            .filter(java.util.Objects::nonNull)
            .toList();

        if (resourceIds.isEmpty()) {
            return Map.of();
        }

        List<ResourceTag> resourceTags = resourceTagRepository.findByResource_IdIn(resourceIds);
        Map<UUID, Tag> uniqueTags = new LinkedHashMap<>();
        resourceTags.forEach(resourceTag -> uniqueTags.put(resourceTag.getTag().getId(), resourceTag.getTag()));

        Map<UUID, TagResponse> tagResponsesById = new HashMap<>();
        tagService.toResponses(List.copyOf(uniqueTags.values()))
            .forEach(tagResponse -> tagResponsesById.put(tagResponse.id(), tagResponse));

        Map<UUID, List<TagResponse>> result = new HashMap<>();
        resourceTags.forEach(resourceTag -> {
            UUID resourceId = resourceTag.getResource().getId();
            TagResponse tag = tagResponsesById.get(resourceTag.getTag().getId());
            if (tag == null) {
                return;
            }
            result.computeIfAbsent(resourceId, ignored -> new ArrayList<>())
                .add(tag);
        });
        return result;
    }
}