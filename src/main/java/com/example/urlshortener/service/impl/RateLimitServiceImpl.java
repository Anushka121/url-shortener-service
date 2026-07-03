package com.example.urlshortener.service.impl;

import com.example.urlshortener.exception.RateLimitExceededException;
import com.example.urlshortener.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final int MAX_REQUESTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void validateRateLimit(String clientIp) {

        String key = "rate_limit:" + clientIp;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count > MAX_REQUESTS) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again later."
            );
        }
    }
}

