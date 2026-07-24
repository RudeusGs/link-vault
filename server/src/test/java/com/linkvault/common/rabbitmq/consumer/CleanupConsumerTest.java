package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.common.rabbitmq.event.CleanupEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CleanupConsumerTest {

    @Test
    void consumeCleanup_ShouldNotThrowException() {
        CleanupConsumer consumer = new CleanupConsumer();
        CleanupEvent event = new CleanupEvent(
            "test_cleanup", UUID.randomUUID(), UUID.randomUUID(), Instant.now()
        );

        // Currently it only logs, so we just verify it doesn't throw
        assertDoesNotThrow(() -> consumer.consumeCleanup(event));
    }
}
