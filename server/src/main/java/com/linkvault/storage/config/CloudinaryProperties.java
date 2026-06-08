package com.linkvault.storage.config;

import com.linkvault.folders.entity.Folder;
import com.linkvault.vaults.entity.Vault;
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
    private String allowedFormats = "jpg,jpeg,png,webp,gif,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,csv,json,xml,yaml,yml,log,java,kt,py,ts,tsx,js,jsx,html,css,scss,sql,sh,ps1,c,cpp,h,hpp,cs,go,rs,php,rb,swift,dart,zip,rar,7z,mp3,wav,mp4,webm,mov";

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