package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.common.rabbitmq.event.CleanupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class CleanupConsumer {

    private static final Logger log = LoggerFactory.getLogger(CleanupConsumer.class);

    @RabbitListener(queues = "${app.rabbitmq.cleanup-queue}")
    public void consumeCleanup(CleanupEvent event) {
        log.info("Processing cleanup event: workspaceId={}, cleanupType={}, targetId={}",
            event.workspaceId(), event.cleanupType(), event.targetId());

        try {
            // Note: Hook into existing cleanup logic if safe methods are added
            log.info("Successfully processed cleanup event (logging only for now) for targetId={}", event.targetId());
        } catch (Exception e) {
            log.error("Failed to process cleanup event: {}", event, e);
            throw e; // Rethrow to trigger retry/DLQ mechanism
        }
    }
}
