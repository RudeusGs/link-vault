package com.linkvault.auth.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String issuer = "link-vault-api";
    private String secret = "";
    private long expirationMinutes = 120;

    @Autowired
    private org.springframework.core.env.Environment environment;

    @PostConstruct
    void validate() {
        boolean isProd = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        if (secret == null || secret.length() < 32) {
            if (isProd) {
                throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters in production");
            } else {
                secret = "fallback-secret-for-development-only-do-not-use-in-prod";
            }
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("APP_JWT_EXPIRATION_MINUTES must be positive");
        }
    }
}