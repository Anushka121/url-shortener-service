package com.example.urlshortener.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
public class ShortCodeGenerator {

    private static final String SALT = "urlshortener#2026$salt!";
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;

    @Value("${app.short-code.length:7}")
    private int codeLength;

    /**
     * Generates a deterministic 7-character alphanumeric short code by hashing
     * the provided URL combined with a fixed salt string using SHA-256.
     * The resulting hash bytes are mapped to characters from the alphanumeric
     * character set to produce the short code.
     *
     * @param url the original URL to hash
     * @return a 7-character alphanumeric short code
     */
    public String generate(String url) {
        try {
            String input = url + SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder shortCode = new StringBuilder();
            int length = codeLength > 0 ? codeLength : SHORT_CODE_LENGTH;

            for (int i = 0; i < length; i++) {
                int index = Math.abs(hashBytes[i % hashBytes.length]) % ALPHANUMERIC.length();
                shortCode.append(ALPHANUMERIC.charAt(index));
            }

            log.debug("Generated short code '{}' for URL: {}", shortCode, url);
            return shortCode.toString();

        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 algorithm not available, falling back to random generation", ex);
            return generateRandom();
        }
    }

    /**
     * Generates a random 7-character alphanumeric short code as a fallback
     * when hashing is unavailable or a collision resolution is needed.
     *
     * @return a random 7-character alphanumeric short code
     */
    public String generateRandom() {
        StringBuilder shortCode = new StringBuilder();
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            // change so // confirmation - incremental +salt ( collision and unique)
            int index = (int) (Math.random() * ALPHANUMERIC.length());
            shortCode.append(ALPHANUMERIC.charAt(index));
        }
        log.debug("Generated random short code: {}", shortCode);
        return shortCode.toString();
    }

    /**
     * Generates a unique short code by appending a suffix to resolve hash collisions.
     * Used when the primary hash-based code is already taken.
     *
     * @param url    the original URL
     * @param suffix a numeric suffix to differentiate the code
     * @return a 7-character alphanumeric short code
     */
    public String generateWithSuffix(String url, int suffix) {
        return generate(url + suffix);
    }
}
