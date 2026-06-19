package com.linkvault.common.rabbitmq;

public final class RabbitMqNames {

    public static final String AUDIT_DLQ = "linkvault.audit.dlq";
    public static final String PREVIEW_DLQ = "linkvault.preview.dlq";
    public static final String NOTIFICATION_DLQ = "linkvault.notification.dlq";
    public static final String CLEANUP_DLQ = "linkvault.cleanup.dlq";

    private RabbitMqNames() {
        // Prevent instantiation
    }
}
