package com.example.urlshortener.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class UrlValidator {

    private static final int MAX_URL_LENGTH = 2048;

    /**
     * Validates that the given URL is syntactically correct and uses an
     * accepted scheme (http or https). Returns true if the URL is valid.
     *
     * @param url the URL string to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        if (url.length() > MAX_URL_LENGTH) {
            log.warn("URL exceeds maximum allowed length of {} characters", MAX_URL_LENGTH);
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                log.warn("URL has unsupported scheme: {}", scheme);
                return false;
            }

            if (uri.getHost() == null || uri.getHost().isBlank()) {
                log.warn("URL has no host: {}", url);
                return false;
            }

            return true;

        } catch (URISyntaxException ex) {
            log.warn("URL syntax validation failed for '{}': {}", url, ex.getMessage());
            return false;
        }
    }
}
