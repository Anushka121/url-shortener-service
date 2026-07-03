
package com.example.urlshortener.service;
public interface RateLimitService {

    void validateRateLimit(String clientIp);
}

