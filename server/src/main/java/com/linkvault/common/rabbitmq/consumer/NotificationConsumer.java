package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.common.rabbitmq.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    @RabbitListener(queues = "${app.rabbitmq.notification-queue}")
    public void consumeNotification(NotificationEvent event) {
        log.info("Processing notification event: workspaceId={}, eventType={}, targetId={}",
            event.workspaceId(), event.eventType(), event.targetId());

        try {
            // Note: Implement actual notification logic when NotificationService is available
            log.info("Successfully processed notification event (logging only for now): {}", event.title());
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event, e);
            throw e; // Rethrow to trigger retry/DLQ mechanism
        }
    }
}
