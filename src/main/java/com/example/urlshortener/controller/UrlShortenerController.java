package com.example.urlshortener.controller;

import com.example.urlshortener.config.AppConfig;
import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(AppConfig.SHORT_URL)
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    public UrlShortenerController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    /**
     * Shortens the provided URL and returns a short code with the full shortened URL.
     *
     * @param request the shorten URL request containing original URL and optional alias
     * @return HTTP 201 CREATED with the shortened URL response
     */
    @PostMapping(AppConfig.SHORTEN)
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        log.info("POST Shorten - received shorten request - correlationId: {}", MDC.get("correlationId"));
        ShortenUrlResponse response = urlShortenerService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves analytics statistics for the given short code.
     *
     * @param code the short code to look up
     * @return HTTP 200 OK with the stats response
     */
    @GetMapping(AppConfig.ANALYTICS)
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable String code) {
        log.info("GET Analytics {} - received stats request - correlationId: {}", code, MDC.get("correlationId"));
        UrlStatsResponse response = urlShortenerService.getStats(code);
        return ResponseEntity.ok(response);
    }
}
