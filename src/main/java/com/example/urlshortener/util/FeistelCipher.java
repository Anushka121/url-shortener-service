package com.example.urlshortener.util;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeistelCipher {

    // 36-bit total space (2^36 ≈ 68.7 billion IDs) split into two 18-bit halves.
    // Comfortably covers million/billion-scale traffic with room to grow.
    private static final int HALF_BITS = 18;
    private static final int HALF_MASK = (1 << HALF_BITS) - 1;
    private static final int ROUNDS = 4;

    @Value("${app.short-code.feistel-key:0x9E3779B9}")
    private long roundKey;

    /**
     * Applies a Feistel network to scramble a sequential ID into a
     * pseudo-random-looking value in the same 36-bit space.
     * Reversible by construction (bijective), so uniqueness of the
     * input ID guarantees uniqueness of the output.
     *
     * @param id the sequential input ID (from Redis INCR)
     * @return a scrambled ID with the same bit width
     */
    public long scramble(long id) {
        long left = (id >> HALF_BITS) & HALF_MASK;
        long right = id & HALF_MASK;

        for (int round = 0; round < ROUNDS; round++) {
            long newLeft = right;
            long newRight = left ^ roundFunction(right, round);
            left = newLeft;
            right = newRight;
        }
        return (left << HALF_BITS) | right;
    }

    private long roundFunction(long value, int round) {
        long mixed = (value * roundKey) ^ (round * 0x85EBCA6BL);
        return mixed & HALF_MASK;
    }
}