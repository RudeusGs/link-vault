package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class AuditLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

    private final AuditLogService auditLogService;

    public AuditLogConsumer(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @RabbitListener(queues = "${app.rabbitmq.audit-queue}")
    public void consumeAuditLog(AuditLogEvent event) {
        log.info("Processing audit log event: workspaceId={}, action={}, targetType={}, targetId={}",
            event.workspaceId(), event.action(), event.targetType(), event.targetId());

        try {
            auditLogService.recordDirect(
                event.workspaceId(),
                event.actorUserId(),
                event.action(),
                event.targetType(),
                event.targetId(),
                event.metadata()
            );
            log.info("Successfully processed audit log event for targetId={}", event.targetId());
        } catch (Exception e) {
            log.error("Failed to process audit log event: {}", event, e);
            throw e; // Rethrow to trigger retry/DLQ mechanism
        }
    }
}
