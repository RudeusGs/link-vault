package com.linkvault.tags;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.resources.ResourceTagRepository;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import java.util.HashMap;
import java.util.Comparator;
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

    public TagService(
        TagRepository tagRepository,
        ResourceTagRepository resourceTagRepository,
        UserContextService userContextService
    ) {
        this.tagRepository = tagRepository;
        this.resourceTagRepository = resourceTagRepository;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags() {
        UUID userId = userContextService.getCurrentUser().getId();
        return toResponses(tagRepository.findByUser_IdOrderByNameAsc(userId));
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        User user = userContextService.getCurrentUser();
        String name = normalizeName(request.name());
        ensureNameAvailable(user.getId(), name, null);

        Tag tag = new Tag();
        tag.setUser(user);
        applyRequest(tag, request);

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(UUID id, TagRequest request) {
        Tag tag = getTag(id);
        String name = normalizeName(request.name());
        ensureNameAvailable(tag.getUser().getId(), name, tag.getName());
        applyRequest(tag, request);
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void delete(UUID id) {
        Tag tag = getTag(id);
        resourceTagRepository.deleteByTag_Id(id);
        tagRepository.delete(tag);
    }

    @Transactional(readOnly = true)
    public Tag getTag(UUID id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.TAG_NOT_FOUND, "Tag not found"));
        ensureOwner(tag);
        return tag;
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

    private void ensureNameAvailable(UUID userId, String normalizedName, String currentName) {
        if (currentName != null && currentName.equalsIgnoreCase(normalizedName)) {
            return;
        }
        if (tagRepository.existsByUser_IdAndNameIgnoreCase(userId, normalizedName)) {
            throw new BadRequestException("Tag name already exists");
        }
    }

    private void ensureOwner(Tag tag) {
        UUID currentUserId = userContextService.getCurrentUser().getId();
        if (!tag.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.TAG_ACCESS_DENIED, "You do not have access to this tag");
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

