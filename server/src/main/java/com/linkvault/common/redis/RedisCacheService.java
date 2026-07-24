package com.linkvault.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisProperties properties;

    public RedisCacheService(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        RedisProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String namespacedKey(String key) {
        String prefix = properties.getKeyPrefix();
        if (prefix == null || prefix.isBlank()) {
            return key;
        }
        return prefix.trim() + ":" + key;
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        return get(key, type).orElseGet(() -> {
            T value = loader.get();
            set(key, value, ttl);
            return value;
        });
    }

    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> typeReference, Supplier<T> loader) {
        return get(key, typeReference).orElseGet(() -> {
            T value = loader.get();
            set(key, value, ttl);
            return value;
        });
    }

    public <T> List<T> getListOrLoad(String key, Duration ttl, Class<T> elementType, Supplier<List<T>> loader) {
        return getList(key, elementType).orElseGet(() -> {
            List<T> value = loader.get();
            set(key, value, ttl);
            return value;
        });
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(namespacedKey(key));
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, type));
        } catch (Exception exception) {
            log.warn("Redis cache read failed for key {}: {}", key, rootMessage(exception));
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(namespacedKey(key));
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, typeReference));
        } catch (Exception exception) {
            log.warn("Redis cache read failed for key {}: {}", key, rootMessage(exception));
            return Optional.empty();
        }
    }

    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            String value = redisTemplate.opsForValue().get(namespacedKey(key));
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return Optional.of(objectMapper.readValue(value, javaType));
        } catch (Exception exception) {
            log.warn("Redis cache list read failed for key {}: {}", key, rootMessage(exception));
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        if (!isEnabled() || value == null) {
            return;
        }
        try {
            Duration safeTtl = ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofMinutes(5) : ttl;
            redisTemplate.opsForValue().set(namespacedKey(key), objectMapper.writeValueAsString(value), safeTtl);
        } catch (JsonProcessingException exception) {
            log.warn("Redis cache serialization failed for key {}: {}", key, exception.getMessage());
        } catch (Exception exception) {
            log.warn("Redis cache write failed for key {}: {}", key, rootMessage(exception));
        }
    }

    public void delete(String key) {
        if (!isEnabled()) {
            return;
        }
        try {
            redisTemplate.delete(namespacedKey(key));
        } catch (Exception exception) {
            log.warn("Redis cache delete failed for key {}: {}", key, rootMessage(exception));
        }
    }

    public void deleteAll(Collection<String> keys) {
        if (!isEnabled() || keys == null || keys.isEmpty()) {
            return;
        }
        try {
            redisTemplate.delete(keys.stream().map(this::namespacedKey).toList());
        } catch (Exception exception) {
            log.warn("Redis cache batch delete failed: {}", rootMessage(exception));
        }
    }

    public void deleteByPattern(String pattern) {
        if (!isEnabled() || pattern == null || pattern.isBlank()) {
            return;
        }
        try {
            String fullPattern = namespacedKey(pattern);
            ScanOptions options = ScanOptions.scanOptions().match(fullPattern).count(100).build();
            redisTemplate.execute((RedisConnection connection) -> {
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        connection.keyCommands().del(cursor.next());
                    }
                }
                return null;
            });
        } catch (Exception exception) {
            log.warn("Redis cache pattern delete failed for pattern {}: {}", pattern, rootMessage(exception));
        }
    }

    private String rootMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null && !(root instanceof RedisConnectionFailureException)) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
