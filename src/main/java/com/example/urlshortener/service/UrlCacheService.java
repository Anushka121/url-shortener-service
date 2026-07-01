package com.example.urlshortener.service;

import com.example.urlshortener.entity.UrlMapping;

import java.util.Optional;

public interface UrlCacheService {

    /**
     * Stores the original URL for the given short code in the Redis cache with a TTL.
     *
     * @param shortCode   the short code key
     * @param originalUrl the original URL value to cache
     */
    void cacheUrl(String shortCode, String originalUrl);

    /**
     * Retrieves the original URL for the given short code from the Redis cache.
     *
     * @param shortCode the short code to look up
     * @return an Optional containing the cached original URL, or empty if not cached
     */
    Optional<String> getCachedUrl(String shortCode);

    /**
     * Removes the cache entry for the given short code.
     *
     * @param shortCode the short code whose cache entry should be evicted
     */
    void evictCache(String shortCode);
}
