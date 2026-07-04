package com.example.urlshortener.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdGenerator {

    private static final String COUNTER_KEY = "url:shortener:global_counter";

    private final StringRedisTemplate redisTemplate;

    /**
     * Atomically increments and returns the next globally unique ID.
     * Redis INCR is single-threaded internally, so this is race-free
     * even under concurrent requests across multiple app instances.
     *
     * @return the next unique, monotonically increasing ID
     */
    public long nextId() {
        Long id = redisTemplate.opsForValue().increment(COUNTER_KEY);
        if (id == null) {
            // Extremely unlikely with a healthy Redis connection, but fail loudly
            log.error("Redis INCR returned null for key '{}'", COUNTER_KEY);
            throw new IllegalStateException("Failed to generate unique ID from Redis");
        }
        return id;
    }
}