package com.linkvault.tags;

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
        UUID userId = userContextService.getDemoUser().getId();
        return tagRepository.findByUser_IdOrderByNameAsc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        User user = userContextService.getDemoUser();

        Tag tag = new Tag();
        tag.setUser(user);
        applyRequest(tag, request);

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(UUID id, TagRequest request) {
        Tag tag = getTag(id);
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
        return tagRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tag not found"));
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

    private void applyRequest(Tag tag, TagRequest request) {
        tag.setName(request.name().trim());
        tag.setColor(request.color());
    }
}
