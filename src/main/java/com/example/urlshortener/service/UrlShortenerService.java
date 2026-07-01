package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;

public interface UrlShortenerService {

    /**
     * Shortens the provided original URL, optionally using a custom alias.
     * Validates the URL, checks for alias conflicts, generates or uses the provided
     * alias, persists the mapping in Cassandra, and caches it in Redis.
     *
     * @param request the request containing the original URL and optional custom alias
     * @return a response containing the short code and the full shortened URL
     */
    ShortenUrlResponse shortenUrl(ShortenUrlRequest request);

    /**
     * Resolves a short code to its original URL using the cache-aside pattern.
     * First checks Redis cache; on miss, queries Cassandra, caches the result,
     * and increments the click counter.
     *
     * @param code the short code to resolve
     * @return the original URL that the short code maps to
     */
    String resolveUrl(String code);

    /**
     * Retrieves analytics statistics for a given short code, including click count
     * and creation timestamp.
     *
     * @param code the short code to retrieve stats for
     * @return the statistics response DTO
     */
    UrlStatsResponse getStats(String code);
}
