package com.example.urlshortener.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "rate-limit")
@Component
@Data
public class RateLimitProperties {

    private int limit;
    private Duration window;
}