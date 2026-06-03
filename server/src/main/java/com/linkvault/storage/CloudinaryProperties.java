package com.linkvault.storage;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private String folder = "link-vault";
    private boolean useSecureUrl = true;
    private long maxFileSizeMb = 20;
    private String allowedFormats = "jpg,jpeg,png,webp,gif,pdf,txt,md,json,java,ts,js,html,css";

    public Set<String> allowedFormatSet() {
        return Arrays.stream(allowedFormats.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    public long maxFileSizeBytes() {
        return maxFileSizeMb * 1024 * 1024;
    }

    public boolean hasCredentials() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
