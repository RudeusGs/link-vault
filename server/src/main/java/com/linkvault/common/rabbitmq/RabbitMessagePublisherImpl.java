package com.linkvault.common.rabbitmq;

import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import com.linkvault.common.rabbitmq.event.CleanupEvent;
import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.common.rabbitmq.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMessagePublisherImpl implements RabbitMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitMessagePublisherImpl.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;

    public RabbitMessagePublisherImpl(RabbitTemplate rabbitTemplate, RabbitMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishAuditLog(AuditLogEvent event) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getAuditRoutingKey(), event);
        log.debug("Published audit log event: {}", event);
    }

    @Override
    public void publishLinkPreviewRequested(LinkPreviewRequestedEvent event) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getPreviewRoutingKey(), event);
        log.debug("Published link preview requested event: {}", event);
    }

    @Override
    public void publishNotification(NotificationEvent event) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getNotificationRoutingKey(), event);
        log.debug("Published notification event: {}", event);
    }

    @Override
    public void publishCleanup(CleanupEvent event) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getCleanupRoutingKey(), event);
        log.debug("Published cleanup event: {}", event);
    }
}
