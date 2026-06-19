package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.audit.service.AuditLogService;
import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogConsumerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogConsumer consumer;

    @Test
    void consumeAuditLog() {
        UUID workspaceId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AuditLogEvent event = new AuditLogEvent(workspaceId, actorId, "test_action", "TEST", targetId, "{}", Instant.now());

        consumer.consumeAuditLog(event);

        verify(auditLogService).recordDirect(workspaceId, actorId, "test_action", "TEST", targetId, "{}");
    }
}
