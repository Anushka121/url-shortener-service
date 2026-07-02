package com.example.urlshortener.service;

import com.example.urlshortener.service.impl.UrlCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlCacheService Unit Tests")
class UrlCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private UrlCacheServiceImpl urlCacheService;

    private final Duration TTL = Duration.ofHours(24);
    private final String SHORT_CODE = "abc1234";
    private final String ORIGINAL_URL = "https://www.google.com";
    private final String CACHE_KEY = "url:" + SHORT_CODE;


    @BeforeEach
    void setUp() {
        urlCacheService = new UrlCacheServiceImpl(redisTemplate, TTL);
        lenient().when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should cache URL with TTL successfully")
    void shouldCacheUrlWithTtlSuccessfully() {
        urlCacheService.cacheUrl(SHORT_CODE, ORIGINAL_URL);

        verify(valueOperations, times(1)).set(CACHE_KEY, ORIGINAL_URL, TTL);
    }

    @Test
    @DisplayName("Should return cached URL on cache hit")
    void shouldReturnCachedUrlOnCacheHit() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(ORIGINAL_URL);

        Optional<String> result = urlCacheService.getCachedUrl(SHORT_CODE);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(ORIGINAL_URL);
    }

    @Test
    @DisplayName("Should return empty Optional on cache miss")
    void shouldReturnEmptyOptionalOnCacheMiss() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);

        Optional<String> result = urlCacheService.getCachedUrl(SHORT_CODE);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should evict cache entry successfully")
    void shouldEvictCacheEntrySuccessfully() {
        when(redisTemplate.delete(CACHE_KEY)).thenReturn(true);

        urlCacheService.evictCache(SHORT_CODE);

        verify(redisTemplate, times(1)).delete(CACHE_KEY);
    }

    @Test
    @DisplayName("Should handle Redis exception gracefully when caching")
    void shouldHandleRedisExceptionGracefullyWhenCaching() {
        doThrow(new RuntimeException("Redis connection refused")).when(valueOperations)
                .set(eq(CACHE_KEY), eq(ORIGINAL_URL), any(Duration.class));

        urlCacheService.cacheUrl(SHORT_CODE, ORIGINAL_URL);
    }

    @Test
    @DisplayName("Should return empty Optional on Redis exception during get")
    void shouldReturnEmptyOnRedisExceptionDuringGet() {
        when(valueOperations.get(CACHE_KEY)).thenThrow(new RuntimeException("Redis unavailable"));

        Optional<String> result = urlCacheService.getCachedUrl(SHORT_CODE);

        assertThat(result).isEmpty();
    }
}
