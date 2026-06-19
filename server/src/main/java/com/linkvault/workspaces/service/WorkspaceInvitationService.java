package com.linkvault.workspaces.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.users.entity.User;
import com.linkvault.users.repository.UserRepository;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.dto.WorkspaceInvitationRequest;
import com.linkvault.workspaces.dto.WorkspaceInvitationResponse;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.entity.WorkspaceInvitation;
import com.linkvault.workspaces.entity.WorkspaceMember;
import com.linkvault.workspaces.enums.InvitationStatus;
import com.linkvault.workspaces.enums.WorkspaceRole;
import com.linkvault.workspaces.repository.WorkspaceInvitationRepository;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceInvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    private final PermissionService permissionService;
    private final QuotaService quotaService;
    private final AuditLogService auditLogService;

    public WorkspaceInvitationService(
        WorkspaceInvitationRepository invitationRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        UserRepository userRepository,
        UserContextService userContextService,
        PermissionService permissionService,
        QuotaService quotaService,
        AuditLogService auditLogService
    ) {
        this.invitationRepository = invitationRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
        this.permissionService = permissionService;
        this.quotaService = quotaService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> list(UUID workspaceId) {
        permissionService.requireAdminOrOwner(workspaceId);
        return invitationRepository.findByWorkspace_IdOrderByCreatedAtDesc(workspaceId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> pendingForCurrentUser() {
        User user = userContextService.getCurrentUser();
        List<String> identifiers = List.of(
            normalizeLookupIdentifier(user.getEmail()),
            normalizeLookupIdentifier(user.getUsername())
        ).stream()
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();

        if (identifiers.isEmpty()) {
            identifiers = List.of("__no_matching_identifier__");
        }

        return invitationRepository.findPendingForUser(
                user.getId(),
                identifiers,
                InvitationStatus.PENDING,
                Instant.now()
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public WorkspaceInvitationResponse invite(UUID workspaceId, WorkspaceInvitationRequest request) {
        User actor = userContextService.getCurrentUser();
        WorkspaceMember actorMember = permissionService.requireAdminOrOwner(workspaceId, actor);
        Workspace workspace = actorMember.getWorkspace();
        quotaService.requireCanAddMember(workspace);

        String identifier = normalizeIdentifier(request.invitedIdentifier());
        WorkspaceRole role = request.role() == null ? WorkspaceRole.MEMBER : request.role();
        if (role == WorkspaceRole.OWNER && actorMember.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException(ErrorCode.WORKSPACE_ROLE_FORBIDDEN, "Only owners can invite owners");
        }

        User invitedUser = resolveInvitedUser(identifier);
        if (invitedUser != null
            && workspaceMemberRepository.existsByWorkspace_IdAndUser_IdAndDeletedAtIsNull(workspaceId, invitedUser.getId())) {
            throw new BadRequestException("User is already a workspace member");
        }

        if (invitationRepository.existsByWorkspace_IdAndInvitedIdentifierIgnoreCaseAndStatus(
            workspaceId,
            identifier,
            InvitationStatus.PENDING
        )) {
            throw new BadRequestException("A pending invitation already exists for this user");
        }

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspace(workspace);
        invitation.setInvitedIdentifier(identifier);
        invitation.setInvitedUser(invitedUser);
        invitation.setInvitedBy(actor);
        invitation.setRole(role);
        invitation.setToken(uniqueToken());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invitation = invitationRepository.save(invitation);

        auditLogService.recordAsync(workspace, actor, "member.invited", "WORKSPACE_INVITATION", invitation.getId());
        return toResponse(invitation);
    }

    @Transactional
    public void cancel(UUID workspaceId, UUID invitationId) {
        User actor = userContextService.getCurrentUser();
        Workspace workspace = permissionService.requireAdminOrOwner(workspaceId, actor).getWorkspace();
        WorkspaceInvitation invitation = invitationRepository.findByIdAndWorkspace_Id(invitationId, workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException(ErrorCode.INVITATION_ALREADY_USED, "Invitation is no longer pending");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
        auditLogService.recordAsync(workspace, actor, "invitation.cancelled", "WORKSPACE_INVITATION", invitation.getId());
    }

    @Transactional
    public WorkspaceInvitationResponse accept(String token) {
        User user = userContextService.getCurrentUser();
        WorkspaceInvitation invitation = validPendingInvitation(token);
        ensureInvitationMatchesUser(invitation, user);

        Workspace workspace = invitation.getWorkspace();
        quotaService.requireCanAddMember(workspace);

        if (workspaceMemberRepository.existsByWorkspace_IdAndUser_IdAndDeletedAtIsNull(workspace.getId(), user.getId())) {
            throw new BadRequestException("User is already a workspace member");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(invitation.getRole());
        workspaceMemberRepository.save(member);

        invitation.setInvitedUser(user);
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        auditLogService.recordAsync(workspace, user, "invitation.accepted", "WORKSPACE_INVITATION", invitation.getId());
        return toResponse(invitation);
    }

    @Transactional
    public WorkspaceInvitationResponse decline(String token) {
        User user = userContextService.getCurrentUser();
        WorkspaceInvitation invitation = validPendingInvitation(token);
        ensureInvitationMatchesUser(invitation, user);

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);
        auditLogService.recordAsync(invitation.getWorkspace(), user, "invitation.declined", "WORKSPACE_INVITATION", invitation.getId());
        return toResponse(invitation);
    }

    private WorkspaceInvitation validPendingInvitation(String token) {
        WorkspaceInvitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException(ErrorCode.INVITATION_NOT_FOUND, "Invitation not found"));

        if (invitation.getWorkspace().getDeletedAt() != null) {
            throw new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND, "Workspace not found");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException(ErrorCode.INVITATION_ALREADY_USED, "Invitation is no longer pending");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException(ErrorCode.INVITATION_EXPIRED, "Invitation has expired");
        }

        return invitation;
    }

    private void ensureInvitationMatchesUser(WorkspaceInvitation invitation, User user) {
        if (invitation.getInvitedUser() != null && !invitation.getInvitedUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorCode.WORKSPACE_ACCESS_DENIED, "This invitation belongs to another user");
        }

        String identifier = invitation.getInvitedIdentifier();
        if (!identifier.equalsIgnoreCase(user.getEmail()) && !identifier.equalsIgnoreCase(user.getUsername())) {
            throw new ForbiddenException(ErrorCode.WORKSPACE_ACCESS_DENIED, "This invitation does not match your account");
        }
    }

    private User resolveInvitedUser(String identifier) {
        return userRepository.findByEmailIgnoreCase(identifier)
            .or(() -> userRepository.findByUsernameIgnoreCase(identifier))
            .orElse(null);
    }

    private WorkspaceInvitationResponse toResponse(WorkspaceInvitation invitation) {
        User invitedUser = invitation.getInvitedUser();
        User invitedBy = invitation.getInvitedBy();
        Workspace workspace = invitation.getWorkspace();
        return new WorkspaceInvitationResponse(
            invitation.getId(),
            workspace.getId(),
            workspace.getName(),
            invitation.getInvitedIdentifier(),
            invitedUser == null ? null : invitedUser.getId(),
            invitedBy.getId(),
            invitedBy.getDisplayName(),
            invitedBy.getUsername(),
            invitation.getRole(),
            invitation.getToken(),
            "/api/workspace-invitations/" + invitation.getToken() + "/accept",
            invitation.getStatus(),
            invitation.getExpiresAt(),
            invitation.getAcceptedAt(),
            invitation.getCreatedAt(),
            invitation.getUpdatedAt()
        );
    }

    private String uniqueToken() {
        String token;
        do {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (invitationRepository.existsByToken(token));
        return token;
    }

    private String normalizeLookupIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Invitation identifier is required");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
