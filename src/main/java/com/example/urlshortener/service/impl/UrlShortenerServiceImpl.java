package com.example.urlshortener.service.impl;

import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.entity.UrlClicks;
import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.mapper.UrlMappingMapper;
import com.example.urlshortener.repository.UrlClicksRepository;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.service.UrlCacheService;
import com.example.urlshortener.service.UrlShortenerService;
import com.example.urlshortener.util.ShortCodeGenerator;
import com.example.urlshortener.util.UrlValidator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final int MAX_COLLISION_RETRIES = 5;

    private final UrlMappingRepository urlMappingRepository;
    private final UrlClicksRepository urlClicksRepository;
    private final UrlCacheService urlCacheService;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlMappingMapper urlMappingMapper;
    private final UrlValidator urlValidator;

    public UrlShortenerServiceImpl(
            UrlMappingRepository urlMappingRepository,
            UrlClicksRepository urlClicksRepository,
            UrlCacheService urlCacheService,
            ShortCodeGenerator shortCodeGenerator,
            UrlMappingMapper urlMappingMapper,
            UrlValidator urlValidator) {

        this.urlMappingRepository = urlMappingRepository;
        this.urlClicksRepository = urlClicksRepository;
        this.urlCacheService = urlCacheService;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlMappingMapper = urlMappingMapper;
        this.urlValidator = urlValidator;
    }

    @Override
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {
        log.info("Creating short URL for request - originalUrl: {}, customAlias: {} - correlationId: {}",
                request.getOriginalUrl(), request.getCustomAlias(), MDC.get("correlationId"));

        if (!urlValidator.isValid(request.getOriginalUrl())) {
            log.error("Invalid URL provided: {} - correlationId: {}",
                    request.getOriginalUrl(), MDC.get("correlationId"));
            throw new InvalidUrlException(request.getOriginalUrl());
        }

        String shortCode;
        boolean isCustomAlias = false;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            shortCode = request.getCustomAlias().trim();
            isCustomAlias = true;

            if (urlMappingRepository.existsById(shortCode)) {
                log.error("Alias already exists: '{}' - correlationId: {}",
                        shortCode, MDC.get("correlationId"));
                throw new AliasAlreadyExistsException(shortCode);
            }
        } else {
            shortCode = resolveUniqueShortCode(request.getOriginalUrl());
        }

        UrlMapping urlMapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .customAlias(isCustomAlias)
                .createdAt(Instant.now())
                .build();

        UrlMapping saved = urlMappingRepository.save(urlMapping);

        log.info("Saved URL mapping - shortCode: '{}' - correlationId: {}",
                shortCode, MDC.get("correlationId"));

        urlCacheService.cacheUrl(shortCode, request.getOriginalUrl());

        ShortenUrlResponse response =
                urlMappingMapper.toShortenUrlResponse(saved);

        log.info("Short URL created successfully - shortUrl: {} - correlationId: {}",
                response.getShortUrl(), MDC.get("correlationId"));

        return response;
    }

    @Override
    public String resolveUrl(String code) {
        log.info("Resolving short code: '{}' - correlationId: {}",
                code, MDC.get("correlationId"));

        var cachedUrl = urlCacheService.getCachedUrl(code);

        if (cachedUrl.isPresent()) {
            log.info("Redis cache hit for code '{}' - correlationId: {}",
                    code, MDC.get("correlationId"));

            incrementClickCount(code);
            return cachedUrl.get();
        }

        log.info("Redis cache miss for code '{}', querying Cassandra - correlationId: {}",
                code, MDC.get("correlationId"));

        UrlMapping urlMapping = urlMappingRepository.findById(code)
                .orElseThrow(() -> {
                    log.error("Short code not found: '{}' - correlationId: {}",
                            code, MDC.get("correlationId"));
                    return new ResourceNotFoundException("UrlMapping", "shortCode", code);
                });

        urlCacheService.cacheUrl(code, urlMapping.getOriginalUrl());

        incrementClickCount(code);

        log.info("Resolved code '{}' to '{}' - correlationId: {}",
                code, urlMapping.getOriginalUrl(), MDC.get("correlationId"));

        return urlMapping.getOriginalUrl();
    }

    @Override
    public UrlStatsResponse getStats(String code) {
        log.info("Fetching stats for short code: '{}' - correlationId: {}",
                code, MDC.get("correlationId"));

        UrlMapping urlMapping = urlMappingRepository.findById(code)
                .orElseThrow(() -> {
                    log.error("Short code not found for stats: '{}' - correlationId: {}",
                            code, MDC.get("correlationId"));
                    return new ResourceNotFoundException("UrlMapping", "shortCode", code);
                });

        Long clickCount = urlClicksRepository.findById(code)
                .map(UrlClicks::getClickCount)
                .orElse(0L);

        log.info("Stats retrieved for code '{}' - clickCount: {} - correlationId: {}",
                code, clickCount, MDC.get("correlationId"));

        return urlMappingMapper.toUrlStatsResponse(urlMapping, clickCount);
    }

    private String resolveUniqueShortCode(String originalUrl) {
        String shortCode = shortCodeGenerator.generate(originalUrl);

        if (!urlMappingRepository.existsById(shortCode)) {
            return shortCode;
        }

        for (int attempt = 1; attempt <= MAX_COLLISION_RETRIES; attempt++) {
            String candidateCode =
                    shortCodeGenerator.generateWithSuffix(originalUrl, attempt);

            if (!urlMappingRepository.existsById(candidateCode)) {
                log.debug("Resolved collision on attempt {} for code '{}' - correlationId: {}",
                        attempt, candidateCode, MDC.get("correlationId"));
                return candidateCode;
            }
        }

        String randomCode = shortCodeGenerator.generateRandom();

        log.warn("Hash collisions exhausted, using random code '{}' - correlationId: {}",
                randomCode, MDC.get("correlationId"));

        return randomCode;
    }

    private void incrementClickCount(String code) {
        try {
            urlClicksRepository.incrementClickCount(code);

            log.debug("Incremented click count for code '{}' - correlationId: {}",
                    code, MDC.get("correlationId"));

        } catch (Exception ex) {
            log.error("Failed to increment click count for code '{}': {} - correlationId: {}",
                    code, ex.getMessage(), MDC.get("correlationId"), ex);
        }
    }
}