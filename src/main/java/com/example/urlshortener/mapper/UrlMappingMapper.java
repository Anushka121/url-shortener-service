package com.example.urlshortener.mapper;

import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.entity.UrlMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlMappingMapper {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Maps a UrlMapping entity to a ShortenUrlResponse DTO, constructing
     * the full shortened URL from the configured base URL and short code.
     *
     * @param urlMapping the entity to convert
     * @return the response DTO containing short code, full short URL, and original URL
     */
    public ShortenUrlResponse toShortenUrlResponse(UrlMapping urlMapping) {
        return ShortenUrlResponse.builder()
                .shortCode(urlMapping.getShortCode())
                .shortUrl(buildShortUrl(urlMapping.getShortCode()))
                .originalUrl(urlMapping.getOriginalUrl())
                .build();
    }

    /**
     * Maps UrlMapping + click count to stats response.
     *
     * @param urlMapping the URL mapping entity
     * @param clickCount click count fetched from url_clicks table
     * @return stats response DTO
     */
    public UrlStatsResponse toUrlStatsResponse(
            UrlMapping urlMapping,
            Long clickCount) {

        return UrlStatsResponse.builder()
                .shortCode(urlMapping.getShortCode())
                .originalUrl(urlMapping.getOriginalUrl())
                .clickCount(clickCount)
                .createdAt(urlMapping.getCreatedAt())
                .build();
    }

    private String buildShortUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }
}