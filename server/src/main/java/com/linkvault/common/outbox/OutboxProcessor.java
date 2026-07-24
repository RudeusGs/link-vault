package com.linkvault.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkvault.common.rabbitmq.RabbitMessagePublisher;
import com.linkvault.common.rabbitmq.event.AuditLogEvent;
import com.linkvault.common.rabbitmq.event.CleanupEvent;
import com.linkvault.common.rabbitmq.event.LinkPreviewRequestedEvent;
import com.linkvault.common.rabbitmq.event.NotificationEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxRepository outboxRepository;
    private final RabbitMessagePublisher rabbitMessagePublisher;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxRepository outboxRepository, RabbitMessagePublisher rabbitMessagePublisher, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitMessagePublisher = rabbitMessagePublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        // Fetch pending or failed events from the last 24 hours
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        List<OutboxEvent> events = outboxRepository.findPendingOrRecentFailed(cutoff, PageRequest.of(0, 100));

        if (events.isEmpty()) {
            return;
        }

        log.debug("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                publishEvent(event);
                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                event.setErrorMessage(null);
            } catch (Exception e) {
                log.error("Failed to process outbox event id={}", event.getId(), e);
                event.setStatus(OutboxStatus.FAILED);
                event.setErrorMessage(e.getMessage());
            }
        }
    }

    private void publishEvent(OutboxEvent event) throws Exception {
        switch (event.getEventType()) {
            case "AuditLogEvent":
                AuditLogEvent auditEvent = objectMapper.readValue(event.getPayload(), AuditLogEvent.class);
                rabbitMessagePublisher.publishAuditLog(auditEvent);
                break;
            case "LinkPreviewRequestedEvent":
                LinkPreviewRequestedEvent previewEvent = objectMapper.readValue(event.getPayload(), LinkPreviewRequestedEvent.class);
                rabbitMessagePublisher.publishLinkPreviewRequested(previewEvent);
                break;
            case "NotificationEvent":
                NotificationEvent notificationEvent = objectMapper.readValue(event.getPayload(), NotificationEvent.class);
                rabbitMessagePublisher.publishNotification(notificationEvent);
                break;
            case "CleanupEvent":
                CleanupEvent cleanupEvent = objectMapper.readValue(event.getPayload(), CleanupEvent.class);
                rabbitMessagePublisher.publishCleanup(cleanupEvent);
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
    }
}
