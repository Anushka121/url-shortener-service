package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlShortenerService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/url")
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }
    /**
     * Resolves a short code to its original URL and redirects the client
     * with HTTP 302 FOUND. Checks Redis cache first, then falls back to Cassandra.
     *
     * @param code the short code to resolve
     * @return HTTP 301 redirect to the original URL
     */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        log.info("GET /{} - redirect request received - correlationId: {}", code, MDC.get("correlationId"));

        String originalUrl = urlShortenerService.resolveUrl(code);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));

        log.info("Redirecting code '{}' to '{}' - correlationId: {}", code, originalUrl, MDC.get("correlationId"));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
