package com.linkvault.common.rabbitmq;

import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import com.linkvault.common.rabbitmq.event.CleanupEvent;
import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.common.rabbitmq.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopRabbitMessagePublisher implements RabbitMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopRabbitMessagePublisher.class);

    public NoopRabbitMessagePublisher() {
        log.info("RabbitMQ is disabled. Using NoopRabbitMessagePublisher.");
    }

    @Override
    public void publishAuditLog(AuditLogEvent event) {
        log.debug("RabbitMQ disabled, ignoring audit log event: {}", event);
    }

    @Override
    public void publishLinkPreviewRequested(LinkPreviewRequestedEvent event) {
        log.debug("RabbitMQ disabled, ignoring link preview requested event: {}", event);
    }

    @Override
    public void publishNotification(NotificationEvent event) {
        log.debug("RabbitMQ disabled, ignoring notification event: {}", event);
    }

    @Override
    public void publishCleanup(CleanupEvent event) {
        log.debug("RabbitMQ disabled, ignoring cleanup event: {}", event);
    }
}
