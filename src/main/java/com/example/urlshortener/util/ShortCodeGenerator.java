package com.example.urlshortener.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortCodeGenerator {

    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;

    private final RedisIdGenerator redisIdGenerator;
    private final FeistelCipher feistelCipher;

    /**
     * Generates a unique 7-character alphanumeric short code.
     * Pulls a monotonically increasing ID from Redis (INCR), scrambles it
     * via a Feistel network to avoid sequential/guessable codes, then
     * encodes the result in Base62. Collision-free by construction -
     * the Feistel permutation is bijective, so a unique input ID guarantees
     * a unique output code. No DB lookup or retry loop needed.
     *
     * @return a 7-character alphanumeric short code
     */
    public String generate() {
        long id = redisIdGenerator.nextId();
        long scrambled = feistelCipher.scramble(id);
        String shortCode = toBase62(scrambled);
        log.debug("Generated short code '{}' for id={} (scrambled={})", shortCode, id, scrambled);
        return shortCode;
    }

    /**
     * Random 7-character alphanumeric short code, used only as a last-resort
     * fallback if Redis is unavailable and ID generation fails entirely.
     *
     * @return a random 7-character alphanumeric short code
     */
    public String generateRandom() {
        StringBuilder shortCode = new StringBuilder();
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = (int) (Math.random() * ALPHANUMERIC.length());
            shortCode.append(ALPHANUMERIC.charAt(index));
        }
        log.debug("Generated random fallback short code: {}", shortCode);
        return shortCode.toString();
    }

    private String toBase62(long value) {
        StringBuilder sb = new StringBuilder();
        if (value == 0) {
            sb.append(ALPHANUMERIC.charAt(0));
        }
        while (value > 0) {
            sb.append(ALPHANUMERIC.charAt((int) (value % ALPHANUMERIC.length())));
            value /= ALPHANUMERIC.length();
        }
        while (sb.length() < SHORT_CODE_LENGTH) {
            sb.append(ALPHANUMERIC.charAt(0));
        }
        return sb.reverse().toString();
    }
}