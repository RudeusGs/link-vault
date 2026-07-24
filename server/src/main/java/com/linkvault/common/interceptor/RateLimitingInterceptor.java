package com.linkvault.common.interceptor;

import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.TooManyRequestsException;
import com.linkvault.common.redis.RedisKeys;
import com.linkvault.common.redis.RedisProperties;
import com.linkvault.common.redis.RedisRateLimiter;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final UserContextService userContextService;
    private final RedisRateLimiter redisRateLimiter;
    private final RedisProperties redisProperties;

    public RateLimitingInterceptor(
        UserContextService userContextService,
        RedisRateLimiter redisRateLimiter,
        RedisProperties redisProperties
    ) {
        this.userContextService = userContextService;
        this.redisRateLimiter = redisRateLimiter;
        this.redisProperties = redisProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        RedisProperties.RateLimit limits = redisProperties.getRateLimit();

        if (path.equals("/api/auth/register")) {
            boolean allowed = redisRateLimiter.consume(
                RedisKeys.rateLimitRegister(extractIp(request)),
                limits.getRegisterCapacity(),
                limits.getRegisterWindow()
            );
            if (!allowed) {
                throw new TooManyRequestsException(ErrorCode.AUTH_TOO_MANY_ATTEMPTS, "Too many registration requests. Please try again later.");
            }
        } else if (path.startsWith("/api/auth")) {
            boolean allowed = redisRateLimiter.consume(
                RedisKeys.rateLimitAuth(extractIp(request)),
                limits.getAuthCapacity(),
                limits.getAuthWindow()
            );
            if (!allowed) {
                throw new TooManyRequestsException(ErrorCode.AUTH_TOO_MANY_ATTEMPTS, "Too many authentication requests");
            }
        } else if (path.contains("/upload")) {
            boolean allowed = redisRateLimiter.consume(
                RedisKeys.rateLimitUpload(currentUserId(request)),
                limits.getUploadCapacity(),
                limits.getUploadWindow()
            );
            if (!allowed) {
                throw new TooManyRequestsException(ErrorCode.GENERAL_TOO_MANY_REQUESTS, "Too many upload requests");
            }
        } else if (path.contains("/preview")) {
            boolean allowed = redisRateLimiter.consume(
                RedisKeys.rateLimitPreview(extractIp(request)),
                limits.getPreviewCapacity(),
                limits.getPreviewWindow()
            );
            if (!allowed) {
                throw new TooManyRequestsException(ErrorCode.GENERAL_TOO_MANY_REQUESTS, "Too many preview requests");
            }
        } else if (path.startsWith("/api/public")) {
            boolean allowed = redisRateLimiter.consume(
                RedisKeys.rateLimitPublic(extractIp(request)),
                limits.getPublicCapacity(),
                limits.getPublicWindow()
            );
            if (!allowed) {
                throw new TooManyRequestsException(ErrorCode.GENERAL_TOO_MANY_REQUESTS, "Too many public requests");
            }
        }

        return true;
    }

    private String extractIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String currentUserId(HttpServletRequest request) {
        try {
            User user = userContextService.getCurrentUser();
            return user.getId().toString();
        } catch (Exception e) {
            return extractIp(request);
        }
    }
}
