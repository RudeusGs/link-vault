package com.linkvault.workspaces.service;

import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.entity.WorkspaceMember;
import com.linkvault.workspaces.enums.WorkspaceRole;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

    private static final EnumSet<WorkspaceRole> EDITORS = EnumSet.of(
        WorkspaceRole.OWNER,
        WorkspaceRole.ADMIN,
        WorkspaceRole.MEMBER
    );

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserContextService userContextService;

    public PermissionService(
        WorkspaceMemberRepository workspaceMemberRepository,
        UserContextService userContextService
    ) {
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireMember(UUID workspaceId) {
        return requireMember(workspaceId, userContextService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireMember(UUID workspaceId, User user) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, user.getId())
            .orElseThrow(() -> new ForbiddenException(
                ErrorCode.WORKSPACE_ACCESS_DENIED,
                "You do not have access to this workspace"
            ));
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireOwner(UUID workspaceId) {
        return requireOwner(workspaceId, userContextService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireOwner(UUID workspaceId, User user) {
        WorkspaceMember member = requireMember(workspaceId, user);
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw roleForbidden("Only workspace owners can perform this action");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireAdminOrOwner(UUID workspaceId) {
        return requireAdminOrOwner(workspaceId, userContextService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireAdminOrOwner(UUID workspaceId, User user) {
        WorkspaceMember member = requireMember(workspaceId, user);
        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.ADMIN) {
            throw roleForbidden("Only workspace owners or admins can perform this action");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireEditor(UUID workspaceId) {
        return requireEditor(workspaceId, userContextService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireEditor(UUID workspaceId, User user) {
        WorkspaceMember member = requireMember(workspaceId, user);
        if (!EDITORS.contains(member.getRole())) {
            throw roleForbidden("You do not have permission to modify this workspace");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public boolean canRead(UUID workspaceId, User user) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, user.getId()).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean canCreate(UUID workspaceId, User user) {
        return canEdit(workspaceId, user);
    }

    @Transactional(readOnly = true)
    public boolean canUpdate(UUID workspaceId, User user) {
        return canEdit(workspaceId, user);
    }

    @Transactional(readOnly = true)
    public boolean canDelete(UUID workspaceId, User user) {
        return canEdit(workspaceId, user);
    }

    public boolean canManageTarget(WorkspaceRole actorRole, WorkspaceRole targetRole) {
        if (actorRole == WorkspaceRole.OWNER) {
            return true;
        }
        return actorRole == WorkspaceRole.ADMIN
            && targetRole != WorkspaceRole.OWNER
            && targetRole != WorkspaceRole.ADMIN;
    }

    public boolean isEditor(WorkspaceRole role) {
        return EDITORS.contains(role);
    }

    private boolean canEdit(UUID workspaceId, User user) {
        return workspaceMemberRepository.findActiveByWorkspaceIdAndUserId(workspaceId, user.getId())
            .map(member -> EDITORS.contains(member.getRole()))
            .orElse(false);
    }

    private ForbiddenException roleForbidden(String message) {
        return new ForbiddenException(ErrorCode.WORKSPACE_ROLE_FORBIDDEN, message);
    }
}