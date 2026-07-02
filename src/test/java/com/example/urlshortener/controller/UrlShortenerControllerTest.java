package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.exception.GlobalExceptionHandler;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShortenerController Unit Tests")
class UrlShortenerControllerTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    @InjectMocks
    private UrlShortenerController urlShortenerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String ORIGINAL_URL =
            "https://www.google.com/very/long/path";

    private static final String SHORT_CODE = "abc1234";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(urlShortenerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST shorten should return 201 CREATED")
    void shouldReturnCreatedWhenShorteningUrl() throws Exception {

        ShortenUrlRequest request =
                ShortenUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .build();

        ShortenUrlResponse response =
                ShortenUrlResponse.builder()
                        .shortCode(SHORT_CODE)
                        .shortUrl(
                                "http://localhost:8080/" + SHORT_CODE
                        )
                        .originalUrl(ORIGINAL_URL)
                        .build();

        when(urlShortenerService
                .shortenUrl(any(ShortenUrlRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/url/shorten")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath(
                                "$.shortCode",
                                is(SHORT_CODE)
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.originalUrl",
                                is(ORIGINAL_URL)
                        )
                );
    }

    @Test
    @DisplayName("POST shorten should return 409 when alias exists")
    void shouldReturnConflictWhenAliasExists()
            throws Exception {

        ShortenUrlRequest request =
                ShortenUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .customAlias("existingAlias")
                        .build();

        when(urlShortenerService
                .shortenUrl(any()))
                .thenThrow(
                        new AliasAlreadyExistsException(
                                "existingAlias"
                        )
                );

        mockMvc.perform(
                        post("/api/v1/url/shorten")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST shorten should return 400 for blank URL")
    void shouldReturn400WhenOriginalUrlIsBlank()
            throws Exception {

        ShortenUrlRequest request =
                ShortenUrlRequest.builder()
                        .originalUrl("")
                        .build();

        mockMvc.perform(
                        post("/api/v1/url/shorten")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET stats should return 200")
    void shouldReturnStatsForValidCode()
            throws Exception {

        UrlStatsResponse stats =
                UrlStatsResponse.builder()
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .clickCount(42L)
                        .createdAt(Instant.now())
                        .build();

        when(urlShortenerService.getStats(SHORT_CODE))
                .thenReturn(stats);

        mockMvc.perform(
                        get("/api/v1/url/stats/" + SHORT_CODE)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.shortCode",
                                is(SHORT_CODE)
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.clickCount",
                                is(42)
                        )
                );
    }

    @Test
    @DisplayName("GET stats should return 404")
    void shouldReturn404WhenCodeNotFoundForStats()
            throws Exception {

        when(urlShortenerService.getStats("unknown"))
                .thenThrow(
                        new ResourceNotFoundException(
                                "UrlMapping",
                                "shortCode",
                                "unknown"
                        )
                );

        mockMvc.perform(
                        get("/api/v1/url/stats/unknown")
                )
                .andExpect(status().isNotFound());
    }
}