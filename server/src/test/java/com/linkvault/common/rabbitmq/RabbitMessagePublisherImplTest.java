package com.linkvault.common.rabbitmq;

import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import com.linkvault.common.rabbitmq.event.CleanupEvent;
import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.common.rabbitmq.event.NotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMessagePublisherImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqProperties properties;
    private RabbitMessagePublisherImpl publisher;

    @BeforeEach
    void setUp() {
        properties = new RabbitMqProperties();
        properties.setExchange("test.exchange");
        properties.setAuditRoutingKey("test.audit.key");
        properties.setPreviewRoutingKey("test.preview.key");
        properties.setNotificationRoutingKey("test.notification.key");
        properties.setCleanupRoutingKey("test.cleanup.key");

        publisher = new RabbitMessagePublisherImpl(rabbitTemplate, properties);
    }

    @Test
    void publishAuditLog() {
        AuditLogEvent event = new AuditLogEvent(UUID.randomUUID(), UUID.randomUUID(), "test_action", "TEST", UUID.randomUUID(), null, Instant.now());
        publisher.publishAuditLog(event);
        verify(rabbitTemplate).convertAndSend("test.exchange", "test.audit.key", event);
    }

    @Test
    void publishLinkPreviewRequested() {
        LinkPreviewRequestedEvent event = new LinkPreviewRequestedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, "https://example.com", false, Instant.now());
        publisher.publishLinkPreviewRequested(event);
        verify(rabbitTemplate).convertAndSend("test.exchange", "test.preview.key", event);
    }

    @Test
    void publishNotification() {
        NotificationEvent event = new NotificationEvent(UUID.randomUUID(), UUID.randomUUID(), "test_event", "title", "msg", UUID.randomUUID(), Instant.now());
        publisher.publishNotification(event);
        verify(rabbitTemplate).convertAndSend("test.exchange", "test.notification.key", event);
    }

    @Test
    void publishCleanup() {
        CleanupEvent event = new CleanupEvent("test_cleanup", UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        publisher.publishCleanup(event);
        verify(rabbitTemplate).convertAndSend("test.exchange", "test.cleanup.key", event);
    }
}
