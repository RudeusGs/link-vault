package com.linkvault.workspaces.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.users.entity.User;
import com.linkvault.users.repository.UserRepository;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.dto.WorkspaceInvitationRequest;
import com.linkvault.workspaces.dto.WorkspaceInvitationResponse;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.entity.WorkspaceInvitation;
import com.linkvault.workspaces.entity.WorkspaceMember;
import com.linkvault.workspaces.enums.InvitationStatus;
import com.linkvault.workspaces.enums.WorkspacePlan;
import com.linkvault.workspaces.enums.WorkspaceRole;
import com.linkvault.workspaces.repository.WorkspaceInvitationRepository;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkspaceInvitationServiceTest {

    private final WorkspaceInvitationRepository invitationRepository = mock(WorkspaceInvitationRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserContextService userContextService = mock(UserContextService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final QuotaService quotaService = mock(QuotaService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final WorkspaceInvitationService invitationService = new WorkspaceInvitationService(
        invitationRepository,
        workspaceMemberRepository,
        userRepository,
        userContextService,
        permissionService,
        quotaService,
        auditLogService
    );

    @Test
    void acceptCreatesWorkspaceMembership() {
        User user = user("ada", "ada@example.com");
        Workspace workspace = workspace(user);
        WorkspaceInvitation invitation = invitation(workspace, user.getEmail(), WorkspaceRole.MEMBER);

        when(userContextService.getCurrentUser()).thenReturn(user);
        when(invitationRepository.findByToken("token")).thenReturn(Optional.of(invitation));
        when(workspaceMemberRepository.existsByWorkspace_IdAndUser_IdAndDeletedAtIsNull(workspace.getId(), user.getId()))
            .thenReturn(false);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.save(any(WorkspaceInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceInvitationResponse response = invitationService.accept("token");

        assertThat(response.status()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    void inviteRejectsExistingWorkspaceMember() {
        User actor = user("owner", "owner@example.com");
        User invited = user("ada", "ada@example.com");
        Workspace workspace = workspace(actor);
        WorkspaceMember actorMember = member(actor, workspace, WorkspaceRole.ADMIN);

        when(userContextService.getCurrentUser()).thenReturn(actor);
        when(permissionService.requireAdminOrOwner(workspace.getId(), actor)).thenReturn(actorMember);
        when(userRepository.findByEmailIgnoreCase(invited.getEmail())).thenReturn(Optional.of(invited));
        when(workspaceMemberRepository.existsByWorkspace_IdAndUser_IdAndDeletedAtIsNull(workspace.getId(), invited.getId()))
            .thenReturn(true);

        assertThatThrownBy(() -> invitationService.invite(
            workspace.getId(),
            new WorkspaceInvitationRequest(invited.getEmail(), WorkspaceRole.MEMBER)
        )).isInstanceOf(BadRequestException.class);
    }

    private WorkspaceInvitation invitation(Workspace workspace, String identifier, WorkspaceRole role) {
        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setWorkspace(workspace);
        invitation.setInvitedIdentifier(identifier);
        invitation.setInvitedBy(workspace.getOwner());
        invitation.setRole(role);
        invitation.setToken("token");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plusSeconds(3600));
        return invitation;
    }

    private WorkspaceMember member(User user, Workspace workspace, WorkspaceRole role) {
        WorkspaceMember member = new WorkspaceMember();
        member.setId(UUID.randomUUID());
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(role);
        return member;
    }

    private User user(String username, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }

    private Workspace workspace(User owner) {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setOwner(owner);
        workspace.setName("Workspace");
        workspace.setSlug("workspace");
        workspace.setPlan(WorkspacePlan.FREE);
        return workspace;
    }
}