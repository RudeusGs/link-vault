package com.linkvault.common.config;

import com.linkvault.common.interceptor.RateLimitingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    public WebMvcConfig(RateLimitingInterceptor rateLimitingInterceptor) {
        this.rateLimitingInterceptor = rateLimitingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
            .addPathPatterns("/api/auth/**")
            .addPathPatterns("/api/workspaces/*/vaults/*/resources/upload", "/api/vaults/*/resources/upload")
            .addPathPatterns("/api/workspaces/*/folders/*/resources/upload", "/api/folders/*/resources/upload")
            .addPathPatterns("/api/resources/*/preview", "/api/workspaces/*/resources/*/preview");
    }
}
