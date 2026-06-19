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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.linkvault.common.rabbitmq.RabbitMessagePublisher;
import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import java.time.Instant;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final PermissionService permissionService;
    private final RabbitMessagePublisher rabbitMessagePublisher;

    public AuditLogService(
        AuditLogRepository auditLogRepository, 
        PermissionService permissionService,
        @Lazy RabbitMessagePublisher rabbitMessagePublisher
    ) {
        this.auditLogRepository = auditLogRepository;
        this.permissionService = permissionService;
        this.rabbitMessagePublisher = rabbitMessagePublisher;
    }

    public void record(Workspace workspace, User actor, String action, String targetType, UUID targetId) {
        recordAsync(workspace, actor, action, targetType, targetId, null);
    }

    public void record(
        Workspace workspace,
        User actor,
        String action,
        String targetType,
        UUID targetId,
        String metadata
    ) {
        recordAsync(workspace, actor, action, targetType, targetId, metadata);
    }

    public void recordAsync(Workspace workspace, User actor, String action, String targetType, UUID targetId) {
        recordAsync(workspace, actor, action, targetType, targetId, null);
    }

    public void recordAsync(Workspace workspace, User actor, String action, String targetType, UUID targetId, String metadata) {
        AuditLogEvent event = new AuditLogEvent(
            workspace.getId(),
            actor != null ? actor.getId() : null,
            action,
            targetType,
            targetId,
            metadata,
            Instant.now()
        );
        rabbitMessagePublisher.publishAuditLog(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDirect(
        UUID workspaceId,
        UUID actorUserId,
        String action,
        String targetType,
        UUID targetId,
        String metadata
    ) {
        try {
            AuditLog auditLog = new AuditLog();
            
            // We use proxy objects here via getReferenceById assuming IDs exist,
            // to avoid extra selects during the async consumer write, since we only need the FK.
            Workspace workspaceRef = new Workspace();
            workspaceRef.setId(workspaceId);
            auditLog.setWorkspace(workspaceRef);
            
            if (actorUserId != null) {
                User actorRef = new User();
                actorRef.setId(actorUserId);
                auditLog.setActor(actorRef);
            }
            
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setMetadata(metadata);
            auditLogRepository.save(auditLog);
        } catch (RuntimeException exception) {
            log.error("Failed to record audit log: action={}, targetType={}, targetId={}", action, targetType, targetId, exception);
            throw exception; // rethrow so consumer retries
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