package com.linkvault.common.redis;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);
    private static final String RATE_LIMIT_SCRIPT = """
        local current = redis.call('INCR', KEYS[1])
        if current == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[2])
        end
        if current > tonumber(ARGV[1]) then
            return -1
        end
        return tonumber(ARGV[1]) - current
        """;

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties properties;
    private final DefaultRedisScript<Long> script;

    public RedisRateLimiter(StringRedisTemplate redisTemplate, RedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
    }

    public boolean consume(String key, int capacity, Duration window) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return true;
        }
        try {
            long seconds = Math.max(1L, window == null ? 60L : window.toSeconds());
            Long remaining = redisTemplate.execute(
                script,
                List.of(namespacedKey(key)),
                String.valueOf(Math.max(1, capacity)),
                String.valueOf(seconds)
            );
            return remaining == null || remaining >= 0;
        } catch (Exception exception) {
            log.warn("Redis rate limit failed for key {}. Request is allowed. Cause: {}", key, exception.getMessage());
            return true;
        }
    }

    private String namespacedKey(String key) {
        String prefix = properties.getKeyPrefix();
        if (prefix == null || prefix.isBlank()) {
            return key;
        }
        return prefix.trim() + ":" + key;
    }
}
