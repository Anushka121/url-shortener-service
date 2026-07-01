package com.example.urlshortener.util;

import java.util.UUID;

public final class CorrelationIdGenerator {

    private CorrelationIdGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a unique correlation ID using UUID for request tracing across services.
     *
     * @return a unique UUID string
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
