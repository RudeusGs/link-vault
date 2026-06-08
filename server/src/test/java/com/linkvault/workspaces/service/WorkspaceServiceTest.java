package com.linkvault.workspaces.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.dto.WorkspaceMemberRoleRequest;
import com.linkvault.workspaces.dto.WorkspaceResponse;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.entity.WorkspaceMember;
import com.linkvault.workspaces.enums.WorkspaceRole;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import com.linkvault.workspaces.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceServiceTest {

    private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final UserContextService userContextService = mock(UserContextService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final QuotaService quotaService = mock(QuotaService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final WorkspaceService workspaceService = new WorkspaceService(
        workspaceRepository,
        workspaceMemberRepository,
        userContextService,
        permissionService,
        quotaService,
        auditLogService
    );

    @Test
    void createDefaultWorkspaceForUserCreatesOwnerMembership() {
        User user = user("ada", "Ada Lovelace");
        when(workspaceRepository.existsBySlugIgnoreCase("ada")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            workspace.setId(UUID.randomUUID());
            return workspace;
        });
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Workspace workspace = workspaceService.createDefaultWorkspaceForUser(user);

        assertThat(workspace.getName()).isEqualTo("Ada Lovelace Workspace");
        assertThat(workspace.getSlug()).isEqualTo("ada");
        assertThat(workspace.getOwner()).isSameAs(user);
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
        verify(auditLogService).record(workspace, user, "workspace.created", "WORKSPACE", workspace.getId());
    }

    @Test
    void listCurrentUserWorkspacesUsesAuthenticatedUserMemberships() {
        User user = user("ada", "Ada Lovelace");
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setOwner(user);
        workspace.setName("Ada Lovelace Workspace");
        workspace.setSlug("ada");

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);

        when(userContextService.getCurrentUser()).thenReturn(user);
        when(workspaceMemberRepository.findActiveByUserId(user.getId())).thenReturn(List.of(member));

        List<WorkspaceResponse> responses = workspaceService.listCurrentUserWorkspaces();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(workspace.getId());
        assertThat(responses.getFirst().role()).isEqualTo(WorkspaceRole.OWNER);
        verify(workspaceMemberRepository).findActiveByUserId(user.getId());
    }

    @Test
    void requireCanWriteRejectsViewerMembership() {
        User user = user("ada", "Ada Lovelace");
        Workspace workspace = workspace(user, "ada");
        WorkspaceMember member = member(user, workspace, WorkspaceRole.VIEWER);

        when(permissionService.requireEditor(workspace.getId())).thenThrow(ForbiddenException.class);

        assertThatThrownBy(() -> workspaceService.requireCanWrite(workspace.getId()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireMemberRejectsUsersOutsideWorkspace() {
        User user = user("ada", "Ada Lovelace");
        UUID workspaceId = UUID.randomUUID();

        when(permissionService.requireMember(workspaceId)).thenThrow(ForbiddenException.class);

        assertThatThrownBy(() -> workspaceService.requireMember(workspaceId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateMemberRoleRejectsDemotingLastOwner() {
        User owner = user("ada", "Ada Lovelace");
        Workspace workspace = workspace(owner, "ada");
        WorkspaceMember ownerMember = member(owner, workspace, WorkspaceRole.OWNER);
        UUID memberId = ownerMember.getId();

        when(userContextService.getCurrentUser()).thenReturn(owner);
        when(permissionService.requireAdminOrOwner(workspace.getId(), owner)).thenReturn(ownerMember);
        when(permissionService.canManageTarget(WorkspaceRole.OWNER, WorkspaceRole.OWNER)).thenReturn(true);
        when(workspaceMemberRepository.findActiveByIdAndWorkspaceId(memberId, workspace.getId()))
            .thenReturn(java.util.Optional.of(ownerMember));
        when(workspaceMemberRepository.countByWorkspace_IdAndRoleAndDeletedAtIsNull(
            workspace.getId(),
            WorkspaceRole.OWNER
        )).thenReturn(1L);

        assertThatThrownBy(() -> workspaceService.updateMemberRole(
            workspace.getId(),
            memberId,
            new WorkspaceMemberRoleRequest(WorkspaceRole.MEMBER)
        )).isInstanceOf(BadRequestException.class);
    }

    private User user(String username, String displayName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setDisplayName(displayName);
        return user;
    }

    private Workspace workspace(User owner, String slug) {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setOwner(owner);
        workspace.setName(owner.getDisplayName() + " Workspace");
        workspace.setSlug(slug);
        return workspace;
    }

    private WorkspaceMember member(User user, Workspace workspace, WorkspaceRole role) {
        WorkspaceMember member = new WorkspaceMember();
        member.setId(UUID.randomUUID());
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(role);
        return member;
    }
}