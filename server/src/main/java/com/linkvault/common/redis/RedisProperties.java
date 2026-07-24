package com.linkvault.common.redis;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {

    private boolean enabled = true;
    private String keyPrefix = "linkvault";
    private Cache cache = new Cache();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Cache {
        private Duration dashboardSummaryTtl = Duration.ofMinutes(5);
        private Duration vaultListTtl = Duration.ofMinutes(10);
        private Duration folderTreeTtl = Duration.ofMinutes(10);
        private Duration resourceListTtl = Duration.ofMinutes(3);
        private Duration resourceDetailTtl = Duration.ofMinutes(5);
        private Duration tagListTtl = Duration.ofMinutes(10);
        private Duration workspaceUsageTtl = Duration.ofMinutes(1);
        private Duration linkPreviewTtl = Duration.ofMinutes(30);
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private int authCapacity = 10;
        private Duration authWindow = Duration.ofMinutes(1);
        private int registerCapacity = 3;
        private Duration registerWindow = Duration.ofHours(1);
        private int uploadCapacity = 20;
        private Duration uploadWindow = Duration.ofMinutes(1);
        private int previewCapacity = 50;
        private Duration previewWindow = Duration.ofMinutes(1);
        private int publicCapacity = 30;
        private Duration publicWindow = Duration.ofMinutes(1);
    }
}
