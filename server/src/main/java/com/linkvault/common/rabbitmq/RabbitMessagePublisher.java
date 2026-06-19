package com.linkvault.common.rabbitmq;

import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import com.linkvault.common.rabbitmq.event.CleanupEvent;
import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.common.rabbitmq.event.NotificationEvent;

public interface RabbitMessagePublisher {

    void publishAuditLog(AuditLogEvent event);

    void publishLinkPreviewRequested(LinkPreviewRequestedEvent event);

    void publishNotification(NotificationEvent event);

    void publishCleanup(CleanupEvent event);
}
