package com.example.urlshortener.controller;

import com.example.urlshortener.exception.GlobalExceptionHandler;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedirectController Unit Tests")
class RedirectControllerTest {

    @Mock
    private UrlShortenerService urlShortenerService;

    @InjectMocks
    private RedirectController redirectController;

    private MockMvc mockMvc;

    private static final String SHORT_CODE = "abc1234";

    private static final String ORIGINAL_URL =
            "https://www.google.com/very/long/path";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(redirectController)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    @DisplayName("GET redirect should return 302 FOUND")
    void shouldReturn302RedirectToOriginalUrl()
            throws Exception {

        when(urlShortenerService
                .resolveUrl(SHORT_CODE))
                .thenReturn(ORIGINAL_URL);

        mockMvc.perform(
                        get("/api/v1/url/" + SHORT_CODE)
                )
                .andExpect(
                        status().isFound()
                )   // 302
                .andExpect(
                        header().string(
                                "Location",
                                ORIGINAL_URL
                        )
                );
    }

    @Test
    @DisplayName("GET redirect should return 404 when code missing")
    void shouldReturn404WhenCodeNotFound()
            throws Exception {

        when(urlShortenerService
                .resolveUrl("notexist"))
                .thenThrow(
                        new ResourceNotFoundException(
                                "UrlMapping",
                                "shortCode",
                                "notexist"
                        )
                );

        mockMvc.perform(
                        get("/api/v1/url/notexist")
                )
                .andExpect(
                        status().isNotFound()
                );
    }
}