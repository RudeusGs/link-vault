package com.linkvault.audit.service;

import com.linkvault.audit.dto.AuditLogResponse;
import com.linkvault.audit.entity.AuditLog;
import com.linkvault.audit.repository.AuditLogRepository;
import com.linkvault.users.entity.User;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.service.PermissionService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final PermissionService permissionService;

    public AuditLogService(AuditLogRepository auditLogRepository, PermissionService permissionService) {
        this.auditLogRepository = auditLogRepository;
        this.permissionService = permissionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Workspace workspace, User actor, String action, String targetType, UUID targetId) {
        record(workspace, actor, action, targetType, targetId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        Workspace workspace,
        User actor,
        String action,
        String targetType,
        UUID targetId,
        String metadata
    ) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setWorkspace(workspace);
            auditLog.setActor(actor);
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setMetadata(metadata);
            auditLogRepository.save(auditLog);
        } catch (RuntimeException exception) {
            log.error("Failed to record audit log: action={}, targetType={}, targetId={}", action, targetType, targetId, exception);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> list(UUID workspaceId) {
        permissionService.requireAdminOrOwner(workspaceId);
        return auditLogRepository.findTop100ByWorkspace_IdOrderByCreatedAtDesc(workspaceId).stream()
            .map(this::toResponse)
            .toList();
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        User actor = auditLog.getActor();
        return new AuditLogResponse(
            auditLog.getId(),
            auditLog.getWorkspace().getId(),
            actor == null ? null : actor.getId(),
            actor == null ? null : actor.getUsername(),
            auditLog.getAction(),
            auditLog.getTargetType(),
            auditLog.getTargetId(),
            auditLog.getMetadata(),
            auditLog.getCreatedAt()
        );
    }
}