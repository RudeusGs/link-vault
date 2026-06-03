package com.linkvault.tags;

import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.resources.ResourceTagRepository;
import com.linkvault.users.User;
import com.linkvault.users.UserContextService;
import java.util.Comparator;
import java.util.List;
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
        return tagRepository.findByUser_IdOrderByNameAsc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        User user = userContextService.getCurrentUser();
        ensureNameAvailable(user.getId(), request.name(), null);

        Tag tag = new Tag();
        tag.setUser(user);
        applyRequest(tag, request);

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(UUID id, TagRequest request) {
        Tag tag = getTag(id);
        ensureNameAvailable(tag.getUser().getId(), request.name(), tag.getName());
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
            .orElseThrow(() -> new NotFoundException("Tag not found"));
        ensureOwner(tag);
        return tag;
    }

    public TagResponse toResponse(Tag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            tag.getId() == null ? 0 : resourceTagRepository.countByTag_Id(tag.getId())
        );
    }

    public List<TagResponse> topTags(List<Tag> tags, int limit) {
        return tags.stream()
            .map(this::toResponse)
            .sorted(Comparator.comparingLong(TagResponse::usageCount).reversed())
            .limit(limit)
            .toList();
    }

    private void ensureNameAvailable(UUID userId, String requestedName, String currentName) {
        String normalized = requestedName == null ? "" : requestedName.trim();
        if (currentName != null && currentName.equalsIgnoreCase(normalized)) {
            return;
        }
        if (tagRepository.existsByUser_IdAndNameIgnoreCase(userId, normalized)) {
            throw new BadRequestException("Tag name already exists");
        }
    }

    private void ensureOwner(Tag tag) {
        UUID currentUserId = userContextService.getCurrentUser().getId();
        if (!tag.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have access to this tag");
        }
    }

    private void applyRequest(Tag tag, TagRequest request) {
        tag.setName(request.name().trim());
        tag.setColor(request.color());
    }
}
