package com.linkvault.workspaces.service;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.exception.BadRequestException;
import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.ForbiddenException;
import com.linkvault.common.exception.NotFoundException;
import com.linkvault.common.redis.RedisCacheInvalidationService;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import com.linkvault.workspaces.dto.WorkspaceMemberResponse;
import com.linkvault.workspaces.dto.WorkspaceMemberRoleRequest;
import com.linkvault.workspaces.dto.WorkspaceRequest;
import com.linkvault.workspaces.dto.WorkspaceResponse;
import com.linkvault.workspaces.dto.WorkspaceUsageResponse;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.entity.WorkspaceMember;
import com.linkvault.workspaces.enums.WorkspacePlan;
import com.linkvault.workspaces.enums.WorkspaceRole;
import com.linkvault.workspaces.repository.WorkspaceMemberRepository;
import com.linkvault.workspaces.repository.WorkspaceRepository;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_SLUG_LENGTH = 120;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserContextService userContextService;
    private final PermissionService permissionService;
    private final QuotaService quotaService;
    private final AuditLogService auditLogService;
    private final RedisCacheInvalidationService cacheInvalidationService;

    public WorkspaceService(
        WorkspaceRepository workspaceRepository,
        WorkspaceMemberRepository workspaceMemberRepository,
        UserContextService userContextService,
        PermissionService permissionService,
        QuotaService quotaService,
        AuditLogService auditLogService,
        RedisCacheInvalidationService cacheInvalidationService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userContextService = userContextService;
        this.permissionService = permissionService;
        this.quotaService = quotaService;
        this.auditLogService = auditLogService;
        this.cacheInvalidationService = cacheInvalidationService;
    }

    @Transactional
    public Workspace createDefaultWorkspaceForUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("Saved user is required to create a default workspace");
        }

        Workspace workspace = new Workspace();
        workspace.setOwner(user);
        workspace.setName(defaultWorkspaceName(user));
        workspace.setSlug(uniqueSlugFor(firstNonBlank(user.getUsername(), user.getDisplayName(), "workspace")));
        workspace.setPlan(WorkspacePlan.FREE);
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMembership = new WorkspaceMember();
        ownerMembership.setWorkspace(savedWorkspace);
        ownerMembership.setUser(user);
        ownerMembership.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(ownerMembership);

        auditLogService.recordAsync(savedWorkspace, user, "workspace.created", "WORKSPACE", savedWorkspace.getId());
        cacheInvalidationService.invalidateWorkspace(savedWorkspace.getId());
        return savedWorkspace;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listCurrentUserWorkspaces() {
        User user = userContextService.getCurrentUser();
        return workspaceMemberRepository.findActiveByUserId(user.getId()).stream()
            .sorted(Comparator.comparing(member -> member.getWorkspace().getName().toLowerCase(Locale.ROOT)))
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspaceResponse(UUID workspaceId) {
        WorkspaceMember member = permissionService.requireMember(workspaceId);
        return toResponse(member);
    }

    @Transactional
    public WorkspaceResponse create(WorkspaceRequest request) {
        User user = userContextService.getCurrentUser();
        Workspace workspace = new Workspace();
        workspace.setOwner(user);
        workspace.setName(normalizeName(request.name()));
        workspace.setSlug(uniqueSlugFor(workspace.getName()));
        workspace.setPlan(WorkspacePlan.FREE);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        auditLogService.recordAsync(workspace, user, "workspace.created", "WORKSPACE", workspace.getId());
        cacheInvalidationService.invalidateWorkspace(workspace.getId());
        return toResponse(member);
    }

    @Transactional
    public WorkspaceResponse update(UUID workspaceId, WorkspaceRequest request) {
        User user = userContextService.getCurrentUser();
        WorkspaceMember member = permissionService.requireOwner(workspaceId, user);
        Workspace workspace = member.getWorkspace();
        workspace.setName(normalizeName(request.name()));
        workspaceRepository.save(workspace);
        auditLogService.recordAsync(workspace, user, "workspace.updated", "WORKSPACE", workspace.getId());
        cacheInvalidationService.invalidateWorkspace(workspace.getId());
        return toResponse(member);
    }

    @Transactional
    public WorkspaceResponse updatePlan(UUID workspaceId, com.linkvault.workspaces.dto.WorkspacePlanRequest request) {
        User user = userContextService.getCurrentUser();
        WorkspaceMember member = permissionService.requireOwner(workspaceId, user);
        Workspace workspace = member.getWorkspace();
        workspace.setPlan(request.plan());
        workspaceRepository.save(workspace);
        auditLogService.recordAsync(workspace, user, "workspace.plan_updated", "WORKSPACE", workspace.getId(), "{\"plan\":\"" + request.plan().name() + "\"}");
        cacheInvalidationService.invalidateWorkspace(workspace.getId());
        return toResponse(member);
    }

    @Transactional
    public void delete(UUID workspaceId) {
        User user = userContextService.getCurrentUser();
        Workspace workspace = permissionService.requireOwner(workspaceId, user).getWorkspace();
        long activeWorkspaceCount = workspaceMemberRepository.countByUser_IdAndDeletedAtIsNullAndWorkspace_DeletedAtIsNull(
            user.getId()
        );
        if (activeWorkspaceCount <= 1) {
            throw new BadRequestException("You cannot delete your last workspace");
        }

        workspaceRepository.delete(workspace);
        auditLogService.recordAsync(workspace, user, "workspace.deleted", "WORKSPACE", workspace.getId());
        cacheInvalidationService.invalidateWorkspace(workspace.getId());
    }

    @Transactional(readOnly = true)
    public Workspace getDefaultWorkspaceForCurrentUser() {
        User user = userContextService.getCurrentUser();
        return workspaceMemberRepository.findFirstByUser_IdAndRoleAndDeletedAtIsNullAndWorkspace_DeletedAtIsNullOrderByWorkspace_CreatedAtAsc(
                user.getId(),
                WorkspaceRole.OWNER
            )
            .or(() -> workspaceMemberRepository.findActiveByUserId(user.getId()).stream().findFirst())
            .map(WorkspaceMember::getWorkspace)
            .orElseThrow(() -> new NotFoundException(
                ErrorCode.WORKSPACE_NOT_FOUND,
                "Default workspace not found"
            ));
    }

    @Transactional(readOnly = true)
    public Workspace getActiveWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND, "Workspace not found"));
    }

    @Transactional(readOnly = true)
    public Workspace requireMember(UUID workspaceId) {
        return permissionService.requireMember(workspaceId).getWorkspace();
    }

    @Transactional(readOnly = true)
    public Workspace requireCanWrite(UUID workspaceId) {
        return permissionService.requireEditor(workspaceId).getWorkspace();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(UUID workspaceId) {
        permissionService.requireMember(workspaceId);
        return workspaceMemberRepository.findActiveByWorkspaceId(workspaceId).stream()
            .map(this::toMemberResponse)
            .toList();
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(
        UUID workspaceId,
        UUID memberId,
        WorkspaceMemberRoleRequest request
    ) {
        User actor = userContextService.getCurrentUser();
        WorkspaceMember actorMember = permissionService.requireAdminOrOwner(workspaceId, actor);
        WorkspaceMember target = getActiveMember(workspaceId, memberId);
        WorkspaceRole newRole = request.role();

        ensureCanManageMember(actorMember, target, newRole);
        ensureNotLastOwnerDemotion(target, newRole);

        target.setRole(newRole);
        workspaceMemberRepository.save(target);
        auditLogService.recordAsync(
            target.getWorkspace(),
            actor,
            "member.role_changed",
            "WORKSPACE_MEMBER",
            target.getId(),
            "{\"role\":\"" + newRole.name() + "\"}"
        );
        cacheInvalidationService.invalidateWorkspace(workspaceId);
        return toMemberResponse(target);
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId) {
        User actor = userContextService.getCurrentUser();
        WorkspaceMember actorMember = permissionService.requireAdminOrOwner(workspaceId, actor);
        WorkspaceMember target = getActiveMember(workspaceId, memberId);

        ensureCanManageMember(actorMember, target, target.getRole());
        ensureNotLastOwnerRemoval(target);

        workspaceMemberRepository.delete(target);
        auditLogService.recordAsync(target.getWorkspace(), actor, "member.removed", "WORKSPACE_MEMBER", target.getId());
        cacheInvalidationService.invalidateWorkspace(workspaceId);
    }

    @Transactional(readOnly = true)
    public WorkspaceUsageResponse usage(UUID workspaceId) {
        Workspace workspace = permissionService.requireMember(workspaceId).getWorkspace();
        return quotaService.usage(workspace);
    }

    private WorkspaceMember getActiveMember(UUID workspaceId, UUID memberId) {
        return workspaceMemberRepository.findActiveByIdAndWorkspaceId(memberId, workspaceId)
            .orElseThrow(() -> new NotFoundException(
                ErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
                "Workspace member not found"
            ));
    }

    private void ensureCanManageMember(
        WorkspaceMember actorMember,
        WorkspaceMember target,
        WorkspaceRole requestedRole
    ) {
        if (!permissionService.canManageTarget(actorMember.getRole(), target.getRole())) {
            throw new ForbiddenException(
                ErrorCode.WORKSPACE_ROLE_FORBIDDEN,
                "You cannot manage this workspace member"
            );
        }

        if (actorMember.getRole() == WorkspaceRole.ADMIN
            && (requestedRole == WorkspaceRole.ADMIN || requestedRole == WorkspaceRole.OWNER)) {
            throw new ForbiddenException(
                ErrorCode.WORKSPACE_ROLE_FORBIDDEN,
                "Admins cannot grant admin or owner roles"
            );
        }
    }

    private void ensureNotLastOwnerDemotion(WorkspaceMember target, WorkspaceRole newRole) {
        if (target.getRole() == WorkspaceRole.OWNER && newRole != WorkspaceRole.OWNER) {
            ensureMoreThanOneOwner(target);
        }
    }

    private void ensureNotLastOwnerRemoval(WorkspaceMember target) {
        if (target.getRole() == WorkspaceRole.OWNER) {
            ensureMoreThanOneOwner(target);
        }
    }

    private void ensureMoreThanOneOwner(WorkspaceMember target) {
        long ownerCount = workspaceMemberRepository.countByWorkspace_IdAndRoleAndDeletedAtIsNull(
            target.getWorkspace().getId(),
            WorkspaceRole.OWNER
        );
        if (ownerCount <= 1) {
            throw new BadRequestException("A workspace must always have at least one owner");
        }
    }

    private WorkspaceResponse toResponse(WorkspaceMember member) {
        Workspace workspace = member.getWorkspace();
        return new WorkspaceResponse(
            workspace.getId(),
            workspace.getName(),
            workspace.getSlug(),
            workspace.getOwner().getId(),
            workspace.getPlan(),
            member.getRole(),
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
        );
    }

    private WorkspaceMemberResponse toMemberResponse(WorkspaceMember member) {
        User user = member.getUser();
        return new WorkspaceMemberResponse(
            member.getId(),
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            member.getRole(),
            member.getCreatedAt()
        );
    }

    private String defaultWorkspaceName(User user) {
        String ownerName = firstNonBlank(user.getDisplayName(), user.getUsername(), "Personal");
        String suffix = " Workspace";
        return truncate(ownerName, MAX_NAME_LENGTH - suffix.length()) + suffix;
    }

    private String uniqueSlugFor(String source) {
        String base = slugify(source);
        String candidate = truncate(base, MAX_SLUG_LENGTH);
        int counter = 2;

        while (workspaceRepository.existsBySlugIgnoreCase(candidate)) {
            String suffix = "-" + counter;
            candidate = truncate(base, MAX_SLUG_LENGTH - suffix.length()) + suffix;
            counter++;
        }

        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");

        return normalized.isBlank() ? "workspace" : normalized;
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Workspace name is required");
        }
        return truncate(value.trim(), MAX_NAME_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}

