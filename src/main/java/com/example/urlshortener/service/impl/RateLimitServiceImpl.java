package com.example.urlshortener.service.impl;

import com.example.urlshortener.config.RateLimitProperties;
import com.example.urlshortener.exception.RateLimitExceededException;
import com.example.urlshortener.service.RateLimitService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitServiceImpl implements RateLimitService {


    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                RateLimitProperties rateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    public void validateRateLimit(String clientIp) {

        String key = "rate_limit:" + clientIp;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, rateLimitProperties.getWindow());
        }

        if (count != null && count > rateLimitProperties.getLimit()) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again later."
            );
        }
    }
}

