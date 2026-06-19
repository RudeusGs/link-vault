package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.resources.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class LinkPreviewConsumer {

    private static final Logger log = LoggerFactory.getLogger(LinkPreviewConsumer.class);

    private final ResourceService resourceService;

    public LinkPreviewConsumer(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @RabbitListener(queues = "${app.rabbitmq.preview-queue}")
    public void consumeLinkPreviewRequested(LinkPreviewRequestedEvent event) {
        log.info("Processing link preview requested event: resourceId={}, url={}, force={}",
            event.resourceId(), event.url(), event.force());

        try {
            resourceService.processLinkPreview(event.resourceId(), event.force());
            log.info("Successfully processed link preview requested event for resourceId={}", event.resourceId());
        } catch (Exception e) {
            log.error("Failed to process link preview requested event: {}", event, e);
            throw e; // Rethrow to trigger retry/DLQ mechanism
        }
    }
}
