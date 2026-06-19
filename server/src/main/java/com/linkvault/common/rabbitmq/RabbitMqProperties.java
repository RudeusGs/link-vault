package com.linkvault.common.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMqProperties {

    private boolean enabled = true;
    private String exchange = "linkvault.events.exchange";
    private String auditQueue = "linkvault.audit.queue";
    private String previewQueue = "linkvault.preview.queue";
    private String notificationQueue = "linkvault.notification.queue";
    private String cleanupQueue = "linkvault.cleanup.queue";
    private String auditRoutingKey = "linkvault.audit.record";
    private String previewRoutingKey = "linkvault.resource.preview.requested";
    private String notificationRoutingKey = "linkvault.notification.requested";
    private String cleanupRoutingKey = "linkvault.cleanup.requested";
    private Retry retry = new Retry();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getAuditQueue() {
        return auditQueue;
    }

    public void setAuditQueue(String auditQueue) {
        this.auditQueue = auditQueue;
    }

    public String getPreviewQueue() {
        return previewQueue;
    }

    public void setPreviewQueue(String previewQueue) {
        this.previewQueue = previewQueue;
    }

    public String getNotificationQueue() {
        return notificationQueue;
    }

    public void setNotificationQueue(String notificationQueue) {
        this.notificationQueue = notificationQueue;
    }

    public String getCleanupQueue() {
        return cleanupQueue;
    }

    public void setCleanupQueue(String cleanupQueue) {
        this.cleanupQueue = cleanupQueue;
    }

    public String getAuditRoutingKey() {
        return auditRoutingKey;
    }

    public void setAuditRoutingKey(String auditRoutingKey) {
        this.auditRoutingKey = auditRoutingKey;
    }

    public String getPreviewRoutingKey() {
        return previewRoutingKey;
    }

    public void setPreviewRoutingKey(String previewRoutingKey) {
        this.previewRoutingKey = previewRoutingKey;
    }

    public String getNotificationRoutingKey() {
        return notificationRoutingKey;
    }

    public void setNotificationRoutingKey(String notificationRoutingKey) {
        this.notificationRoutingKey = notificationRoutingKey;
    }

    public String getCleanupRoutingKey() {
        return cleanupRoutingKey;
    }

    public void setCleanupRoutingKey(String cleanupRoutingKey) {
        this.cleanupRoutingKey = cleanupRoutingKey;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public static class Retry {
        private int maxAttempts = 3;
        private long initialInterval = 1000;
        private double multiplier = 2.0;
        private long maxInterval = 10000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialInterval() {
            return initialInterval;
        }

        public void setInitialInterval(long initialInterval) {
            this.initialInterval = initialInterval;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxInterval() {
            return maxInterval;
        }

        public void setMaxInterval(long maxInterval) {
            this.maxInterval = maxInterval;
        }
    }
}
