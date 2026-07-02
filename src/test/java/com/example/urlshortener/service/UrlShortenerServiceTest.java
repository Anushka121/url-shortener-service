package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.entity.UrlClicks;
import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.mapper.UrlMappingMapper;
import com.example.urlshortener.repository.UrlClicksRepository;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.service.impl.UrlShortenerServiceImpl;
import com.example.urlshortener.util.ShortCodeGenerator;
import com.example.urlshortener.util.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShortenerService Unit Tests")
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private UrlClicksRepository urlClicksRepository;

    @Mock
    private UrlCacheService urlCacheService;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private UrlMappingMapper urlMappingMapper;

    @Mock
    private UrlValidator urlValidator;

    @InjectMocks
    private UrlShortenerServiceImpl urlShortenerService;

    private UrlMapping sampleMapping;
    private ShortenUrlResponse sampleResponse;

    private static final String ORIGINAL_URL =
            "https://www.google.com/very/long/path";

    private static final String SHORT_CODE = "abc1234";
    private static final String CUSTOM_ALIAS = "google123";

    @BeforeEach
    void setUp() {
        sampleMapping = UrlMapping.builder()
                .shortCode(SHORT_CODE)
                .originalUrl(ORIGINAL_URL)
                .customAlias(false)
                .createdAt(Instant.now())
                .build();

        sampleResponse = ShortenUrlResponse.builder()
                .shortCode(SHORT_CODE)
                .shortUrl("http://localhost:8080/" + SHORT_CODE)
                .originalUrl(ORIGINAL_URL)
                .build();
    }

    @Test
    void shouldShortenUrlSuccessfullyWithoutAlias() {
        ShortenUrlRequest request =
                ShortenUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .build();

        when(urlValidator.isValid(ORIGINAL_URL)).thenReturn(true);
        when(shortCodeGenerator.generate(ORIGINAL_URL))
                .thenReturn(SHORT_CODE);
        when(urlMappingRepository.existsById(SHORT_CODE))
                .thenReturn(false);
        when(urlMappingRepository.save(any()))
                .thenReturn(sampleMapping);
        when(urlMappingMapper.toShortenUrlResponse(sampleMapping))
                .thenReturn(sampleResponse);

        ShortenUrlResponse result =
                urlShortenerService.shortenUrl(request);

        assertThat(result).isNotNull();
        assertThat(result.getShortCode()).isEqualTo(SHORT_CODE);

        verify(urlCacheService).cacheUrl(SHORT_CODE, ORIGINAL_URL);
    }

    @Test
    void shouldThrowInvalidUrlException() {
        ShortenUrlRequest request =
                ShortenUrlRequest.builder()
                        .originalUrl("invalid")
                        .build();

        when(urlValidator.isValid("invalid")).thenReturn(false);

        assertThatThrownBy(() ->
                urlShortenerService.shortenUrl(request))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void shouldThrowAliasAlreadyExistsException() {
        ShortenUrlRequest request =
                ShortenUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .customAlias(CUSTOM_ALIAS)
                        .build();

        when(urlValidator.isValid(ORIGINAL_URL)).thenReturn(true);
        when(urlMappingRepository.existsById(CUSTOM_ALIAS))
                .thenReturn(true);

        assertThatThrownBy(() ->
                urlShortenerService.shortenUrl(request))
                .isInstanceOf(AliasAlreadyExistsException.class);
    }

    @Test
    void shouldResolveUrlFromCache() {
        when(urlCacheService.getCachedUrl(SHORT_CODE))
                .thenReturn(Optional.of(ORIGINAL_URL));

        String result =
                urlShortenerService.resolveUrl(SHORT_CODE);

        assertThat(result).isEqualTo(ORIGINAL_URL);

        verify(urlClicksRepository)
                .incrementClickCount(SHORT_CODE);
    }

    @Test
    void shouldResolveUrlFromDatabase() {
        when(urlCacheService.getCachedUrl(SHORT_CODE))
                .thenReturn(Optional.empty());

        when(urlMappingRepository.findById(SHORT_CODE))
                .thenReturn(Optional.of(sampleMapping));

        String result =
                urlShortenerService.resolveUrl(SHORT_CODE);

        assertThat(result).isEqualTo(ORIGINAL_URL);

        verify(urlCacheService)
                .cacheUrl(SHORT_CODE, ORIGINAL_URL);
    }

    @Test
    void shouldThrowNotFoundWhenCodeMissing() {
        when(urlCacheService.getCachedUrl(SHORT_CODE))
                .thenReturn(Optional.empty());

        when(urlMappingRepository.findById(SHORT_CODE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                urlShortenerService.resolveUrl(SHORT_CODE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnStats() {

        UrlClicks clicks = UrlClicks.builder()
                .shortCode(SHORT_CODE)
                .clickCount(42L)
                .build();

        UrlStatsResponse stats =
                UrlStatsResponse.builder()
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .clickCount(42L)
                        .createdAt(Instant.now())
                        .build();

        when(urlMappingRepository.findById(SHORT_CODE))
                .thenReturn(Optional.of(sampleMapping));

        when(urlClicksRepository.findById(SHORT_CODE))
                .thenReturn(Optional.of(clicks));

        when(urlMappingMapper.toUrlStatsResponse(
                sampleMapping, 42L))
                .thenReturn(stats);

        UrlStatsResponse result =
                urlShortenerService.getStats(SHORT_CODE);

        assertThat(result.getClickCount())
                .isEqualTo(42L);
    }

    @Test
    void shouldResolveCollision() {

        String alternateCode = "xyz789";

        ShortenUrlRequest request =
                ShortenUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .build();

        UrlMapping altMapping =
                UrlMapping.builder()
                        .shortCode(alternateCode)
                        .originalUrl(ORIGINAL_URL)
                        .customAlias(false)
                        .createdAt(Instant.now())
                        .build();

        ShortenUrlResponse altResponse =
                ShortenUrlResponse.builder()
                        .shortCode(alternateCode)
                        .shortUrl("http://localhost/" + alternateCode)
                        .originalUrl(ORIGINAL_URL)
                        .build();

        when(urlValidator.isValid(ORIGINAL_URL))
                .thenReturn(true);

        when(shortCodeGenerator.generate(ORIGINAL_URL))
                .thenReturn(SHORT_CODE);

        when(urlMappingRepository.existsById(SHORT_CODE))
                .thenReturn(true);

        when(shortCodeGenerator.generateWithSuffix(
                ORIGINAL_URL, 1))
                .thenReturn(alternateCode);

        when(urlMappingRepository.existsById(alternateCode))
                .thenReturn(false);

        when(urlMappingRepository.save(any()))
                .thenReturn(altMapping);

        when(urlMappingMapper.toShortenUrlResponse(altMapping))
                .thenReturn(altResponse);

        ShortenUrlResponse result =
                urlShortenerService.shortenUrl(request);

        assertThat(result.getShortCode())
                .isEqualTo(alternateCode);
    }
}