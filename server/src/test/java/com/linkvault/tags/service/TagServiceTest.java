package com.linkvault.tags.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.resources.repository.ResourceTagRepository;
import com.linkvault.tags.dto.TagRequest;
import com.linkvault.tags.dto.TagResponse;
import com.linkvault.tags.entity.Tag;
import com.linkvault.tags.repository.TagRepository;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.service.WorkspaceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagServiceTest {

    private final TagRepository tagRepository = mock(TagRepository.class);
    private final ResourceTagRepository resourceTagRepository = mock(ResourceTagRepository.class);
    private final UserContextService userContextService = mock(UserContextService.class);
    private final WorkspaceService workspaceService = mock(WorkspaceService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final TagService tagService = new TagService(
        tagRepository,
        resourceTagRepository,
        userContextService,
        workspaceService,
        auditLogService
    );

    @Test
    void createScopesTagsToWorkspaceAndChecksNameThere() {
        User user = user("ada");
        Workspace workspace = workspace(user);
        when(userContextService.getCurrentUser()).thenReturn(user);
        when(workspaceService.requireCanWrite(workspace.getId())).thenReturn(workspace);
        when(tagRepository.existsByWorkspace_IdAndNameIgnoreCase(workspace.getId(), "Research")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(UUID.randomUUID());
            return tag;
        });

        TagResponse response = tagService.create(workspace.getId(), new TagRequest(" Research ", "#123456"));

        assertThat(response.name()).isEqualTo("Research");
        verify(tagRepository).existsByWorkspace_IdAndNameIgnoreCase(workspace.getId(), "Research");
        verify(tagRepository).save(any(Tag.class));
    }

    private User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private Workspace workspace(User owner) {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setOwner(owner);
        workspace.setName("Ada Workspace");
        workspace.setSlug("ada");
        return workspace;
    }
}