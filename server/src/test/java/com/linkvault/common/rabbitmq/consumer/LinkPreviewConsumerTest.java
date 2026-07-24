package com.linkvault.common.rabbitmq.consumer;

import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.resources.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LinkPreviewConsumerTest {

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private LinkPreviewConsumer consumer;

    @Test
    void consumeLinkPreviewRequested() {
        UUID resourceId = UUID.randomUUID();
        LinkPreviewRequestedEvent event = new LinkPreviewRequestedEvent(
            resourceId, UUID.randomUUID(), UUID.randomUUID(), null, "https://example.com", true, Instant.now()
        );

        consumer.consumeLinkPreviewRequested(event);

        verify(resourceService).processLinkPreview(resourceId, "https://example.com", true);
    }
}
