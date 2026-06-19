package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.common.rabbitmq.event.NotificationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationConsumerTest {

    private final NotificationConsumer consumer = new NotificationConsumer();

    @Test
    void consumeNotification_ShouldNotThrowException() {
        NotificationEvent event = new NotificationEvent(
            UUID.randomUUID(), UUID.randomUUID(), "test_event", "title", "message", UUID.randomUUID(), Instant.now()
        );

        // Currently it only logs, so we just verify it doesn't throw
        assertDoesNotThrow(() -> consumer.consumeNotification(event));
    }
}
