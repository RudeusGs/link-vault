package com.linkvault.auth.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String issuer = "link-vault-api";
    private String secret = "";
    private long expirationMinutes = 120;

    @PostConstruct
    void validate() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("APP_JWT_EXPIRATION_MINUTES must be positive");
        }
    }
}
