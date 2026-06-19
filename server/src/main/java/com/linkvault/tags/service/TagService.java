package com.linkvault.tags.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.redis.RedisCacheInvalidationService;
import com.linkvault.common.redis.RedisCacheService;
import com.linkvault.common.redis.RedisKeys;
import com.linkvault.common.redis.RedisProperties;
import com.linkvault.resources.repository.ResourceTagRepository;
import com.linkvault.tags.dto.TagRequest;
import com.linkvault.tags.dto.TagResponse;
import com.linkvault.tags.entity.Tag;
import com.linkvault.tags.repository.TagRepository;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.service.WorkspaceService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ResourceTagRepository resourceTagRepository;
    private final UserContextService userContextService;
    private final WorkspaceService workspaceService;
    private final AuditLogService auditLogService;
    private final RedisCacheService redisCacheService;
    private final RedisProperties redisProperties;
    private final RedisCacheInvalidationService cacheInvalidationService;

    public TagService(
        TagRepository tagRepository,
        ResourceTagRepository resourceTagRepository,
        UserContextService userContextService,
        WorkspaceService workspaceService,
        AuditLogService auditLogService,
        RedisCacheService redisCacheService,
        RedisProperties redisProperties,
        RedisCacheInvalidationService cacheInvalidationService
    ) {
        this.tagRepository = tagRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.userContextService = userContextService;
        this.workspaceService = workspaceService;
        this.auditLogService = auditLogService;
        this.redisCacheService = redisCacheService;
        this.redisProperties = redisProperties;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags() {
        Workspace workspace = workspaceService.getDefaultWorkspaceForCurrentUser();
        return listTags(workspace.getId());
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags(UUID workspaceId) {
        workspaceService.requireMember(workspaceId);
        return redisCacheService.getListOrLoad(
            RedisKeys.workspaceTags(workspaceId),
            redisProperties.getCache().getTagListTtl(),
            TagResponse.class,
            () -> toResponses(tagRepository.findByWorkspace_IdOrderByNameAsc(workspaceId))
        );
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        Workspace workspace = workspaceService.getDefaultWorkspaceForCurrentUser();
        return create(workspace.getId(), request);
    }

    @Transactional
    public TagResponse create(UUID workspaceId, TagRequest request) {
        User user = userContextService.getCurrentUser();
        Workspace workspace = workspaceService.requireCanWrite(workspaceId);
        String name = normalizeName(request.name());
        ensureNameAvailable(workspace.getId(), name, null);

        Tag tag = new Tag();
        tag.setUser(user);
        tag.setWorkspace(workspace);
        applyRequest(tag, request);

        Tag savedTag = tagRepository.save(tag);
        auditLogService.recordAsync(workspace, user, "tag.created", "TAG", savedTag.getId());
        invalidateTagCaches(savedTag.getWorkspace().getId());
        return toResponse(savedTag);
    }

    @Transactional
    public TagResponse update(UUID id, TagRequest request) {
        Tag tag = getTagForWrite(id);
        String name = normalizeName(request.name());
        ensureNameAvailable(tag.getWorkspace().getId(), name, tag.getName());
        applyRequest(tag, request);
        Tag savedTag = tagRepository.save(tag);
        auditLogService.recordAsync(savedTag.getWorkspace(), userContextService.getCurrentUser(), "tag.updated", "TAG", savedTag.getId());
        invalidateTagCaches(savedTag.getWorkspace().getId());
        return toResponse(savedTag);
    }

    @Transactional
    public TagResponse update(UUID workspaceId, UUID id, TagRequest request) {
        Tag tag = getTagForWrite(workspaceId, id);
        String name = normalizeName(request.name());
        ensureNameAvailable(tag.getWorkspace().getId(), name, tag.getName());
        applyRequest(tag, request);
        Tag savedTag = tagRepository.save(tag);
        auditLogService.recordAsync(savedTag.getWorkspace(), userContextService.getCurrentUser(), "tag.updated", "TAG", savedTag.getId());
        invalidateTagCaches(savedTag.getWorkspace().getId());
        return toResponse(savedTag);
    }

    @Transactional
    public void delete(UUID id) {
        Tag tag = getTagForWrite(id);
        resourceTagRepository.deleteByTag_Id(id);
        tagRepository.delete(tag);
        auditLogService.recordAsync(tag.getWorkspace(), userContextService.getCurrentUser(), "tag.deleted", "TAG", id);
        invalidateTagCaches(tag.getWorkspace().getId());
    }

    @Transactional
    public void delete(UUID workspaceId, UUID id) {
        Tag tag = getTagForWrite(workspaceId, id);
        resourceTagRepository.deleteByTag_Id(id);
        tagRepository.delete(tag);
        auditLogService.recordAsync(tag.getWorkspace(), userContextService.getCurrentUser(), "tag.deleted", "TAG", id);
        invalidateTagCaches(tag.getWorkspace().getId());
    }

    @Transactional(readOnly = true)
    public Tag getTag(UUID id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TAG_NOT_FOUND, "Tag not found"));
        workspaceService.requireMember(tag.getWorkspace().getId());
        return tag;
    }

    @Transactional(readOnly = true)
    public Tag getTag(UUID workspaceId, UUID id) {
        workspaceService.requireMember(workspaceId);
        return tagRepository.findByIdAndWorkspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TAG_NOT_FOUND, "Tag not found"));
    }

    @Transactional(readOnly = true)
    public Tag getTagForWrite(UUID id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TAG_NOT_FOUND, "Tag not found"));
        workspaceService.requireCanWrite(tag.getWorkspace().getId());
        return tag;
    }

    @Transactional(readOnly = true)
    public Tag getTagForWrite(UUID workspaceId, UUID id) {
        workspaceService.requireCanWrite(workspaceId);
        return tagRepository.findByIdAndWorkspace_Id(id, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TAG_NOT_FOUND, "Tag not found"));
    }

    public TagResponse toResponse(Tag tag) {
        return toResponse(tag, usageByTagId(List.of(tag)));
    }

    public List<TagResponse> toResponses(List<Tag> tags) {
        Map<UUID, Long> usageByTagId = usageByTagId(tags);
        return tags.stream()
            .map(tag -> toResponse(tag, usageByTagId))
            .toList();
    }

    private TagResponse toResponse(Tag tag, Map<UUID, Long> usageByTagId) {
        return new TagResponse(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            usageByTagId.getOrDefault(tag.getId(), 0L)
        );
    }

    public List<TagResponse> topTags(List<Tag> tags, int limit) {
        return toResponses(tags).stream()
            .sorted(Comparator.comparingLong(TagResponse::usageCount).reversed())
            .limit(limit)
            .toList();
    }


    public void invalidateTagCaches(UUID workspaceId) {
        cacheInvalidationService.invalidateWorkspace(workspaceId);
    }

    private Map<UUID, Long> usageByTagId(List<Tag> tags) {
        List<UUID> tagIds = tags.stream()
            .map(Tag::getId)
            .filter(java.util.Objects::nonNull)
            .toList();

        if (tagIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> usage = new HashMap<>();
        resourceTagRepository.countUsageByTagIds(tagIds).forEach(row ->
            usage.put((UUID) row[0], (Long) row[1])
        );
        return usage;
    }

    private void ensureNameAvailable(UUID workspaceId, String normalizedName, String currentName) {
        if (currentName != null && currentName.equalsIgnoreCase(normalizedName)) {
            return;
        }
        if (tagRepository.existsByWorkspace_IdAndNameIgnoreCase(workspaceId, normalizedName)) {
            throw new BadRequestException("Tag name already exists");
        }
    }

    private void applyRequest(Tag tag, TagRequest request) {
        tag.setName(normalizeName(request.name()));
        tag.setColor(normalizeColor(request.color()));
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Tag name is required");
        }
        return value.trim();
    }

    private String normalizeColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

