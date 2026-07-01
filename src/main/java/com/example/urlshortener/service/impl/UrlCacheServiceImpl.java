package com.example.urlshortener.service.impl;

import com.example.urlshortener.service.UrlCacheService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class UrlCacheServiceImpl implements UrlCacheService {

    private static final String CACHE_KEY_PREFIX = "url:";

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration redisTtl;

    public UrlCacheServiceImpl(RedisTemplate<String, String> redisTemplate, Duration redisTtl) {
        this.redisTemplate = redisTemplate;
        this.redisTtl = redisTtl;
    }

    @Override
    public void cacheUrl(String shortCode, String originalUrl) {
        String cacheKey = buildCacheKey(shortCode);
        try {
            redisTemplate.opsForValue().set(cacheKey, originalUrl, redisTtl);
            log.info("Cached URL for short code '{}' with TTL {} hours - correlationId: {}",
                    shortCode, redisTtl.toHours(), MDC.get("correlationId"));
        } catch (Exception ex) {
            log.error("Failed to cache URL for short code '{}': {} - correlationId: {}",
                    shortCode, ex.getMessage(), MDC.get("correlationId"), ex);
        }
    }

    @Override
    public Optional<String> getCachedUrl(String shortCode) {
        String cacheKey = buildCacheKey(shortCode);
        try {
            String cachedValue = redisTemplate.opsForValue().get(cacheKey);
            if (cachedValue != null) {
                log.info("Redis cache hit for code '{}' - correlationId: {}", shortCode, MDC.get("correlationId"));
                return Optional.of(cachedValue);
            }
            log.info("Redis cache miss for code '{}' - correlationId: {}", shortCode, MDC.get("correlationId"));
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Failed to retrieve cached URL for short code '{}': {} - correlationId: {}",
                    shortCode, ex.getMessage(), MDC.get("correlationId"), ex);
            return Optional.empty();
        }
    }

    @Override
    public void evictCache(String shortCode) {
        String cacheKey = buildCacheKey(shortCode);
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Evicted cache entry for short code '{}' - correlationId: {}",
                        shortCode, MDC.get("correlationId"));
            }
        } catch (Exception ex) {
            log.error("Failed to evict cache for short code '{}': {} - correlationId: {}",
                    shortCode, ex.getMessage(), MDC.get("correlationId"), ex);
        }
    }

    private String buildCacheKey(String shortCode) {
        return CACHE_KEY_PREFIX + shortCode;
    }
}
