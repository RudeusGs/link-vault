package com.linkvault.common.interceptor;

import com.linkvault.common.exception.ErrorCode;
import com.linkvault.common.exception.TooManyRequestsException;
import com.linkvault.users.entity.User;
import com.linkvault.users.service.UserContextService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final UserContextService userContextService;

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> uploadBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> previewBuckets = new ConcurrentHashMap<>();

    public RateLimitingInterceptor(UserContextService userContextService) {
        this.userContextService = userContextService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        if (path.startsWith("/api/auth")) {
            String ip = extractIp(request);
            Bucket bucket = authBuckets.computeIfAbsent(ip, this::newAuthBucket);
            if (!bucket.tryConsume(1)) {
                throw new TooManyRequestsException(ErrorCode.AUTH_TOO_MANY_ATTEMPTS, "Too many authentication requests");
            }
        } else if (path.contains("/upload")) {
            String userId = currentUserId(request);
            Bucket bucket = uploadBuckets.computeIfAbsent(userId, this::newUploadBucket);
            if (!bucket.tryConsume(1)) {
                throw new TooManyRequestsException(ErrorCode.GENERAL_TOO_MANY_REQUESTS, "Too many upload requests");
            }
        } else if (path.contains("/preview")) {
            String ip = extractIp(request);
            Bucket bucket = previewBuckets.computeIfAbsent(ip, this::newPreviewBucket);
            if (!bucket.tryConsume(1)) {
                throw new TooManyRequestsException(ErrorCode.GENERAL_TOO_MANY_REQUESTS, "Too many preview requests");
            }
        }

        return true;
    }

    private Bucket newAuthBucket(String key) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build())
            .build();
    }

    private Bucket newUploadBucket(String key) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(20).refillIntervally(20, Duration.ofMinutes(1)).build())
            .build();
    }

    private Bucket newPreviewBucket(String key) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(50).refillIntervally(50, Duration.ofMinutes(1)).build())
            .build();
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
