package com.example.urlshortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShortCodeGenerator Unit Tests")
class ShortCodeGeneratorTest {

    private ShortCodeGenerator shortCodeGenerator;

    @BeforeEach
    void setUp() {
        shortCodeGenerator = new ShortCodeGenerator();
    }

    @Test
    @DisplayName("Should generate a 7-character alphanumeric code")
    void shouldGenerateSevenCharAlphanumericCode() {
        String code = shortCodeGenerator.generate("https://www.example.com");

        assertThat(code).isNotNull();
        assertThat(code).hasSize(7);
        assertThat(code).matches("[a-zA-Z0-9]+");
    }

    @Test
    @DisplayName("Should generate deterministic codes for same input")
    void shouldGenerateDeterministicCodesForSameInput() {
        String url = "https://www.example.com/some/path";
        String code1 = shortCodeGenerator.generate(url);
        String code2 = shortCodeGenerator.generate(url);

        assertThat(code1).isEqualTo(code2);
    }

    @Test
    @DisplayName("Should generate different codes for different URLs")
    void shouldGenerateDifferentCodesForDifferentUrls() {
        String code1 = shortCodeGenerator.generate("https://www.example1.com");
        String code2 = shortCodeGenerator.generate("https://www.example2.com");

        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    @DisplayName("Should generate random code of 7 characters")
    void shouldGenerateRandomCodeOfSevenChars() {
        String code = shortCodeGenerator.generateRandom();

        assertThat(code).isNotNull();
        assertThat(code).hasSize(7);
        assertThat(code).matches("[a-zA-Z0-9]+");
    }

    @Test
    @DisplayName("Should generate different code with suffix")
    void shouldGenerateDifferentCodeWithSuffix() {
        String url = "https://www.example.com";
        String code = shortCodeGenerator.generate(url);
        String codeWithSuffix = shortCodeGenerator.generateWithSuffix(url, 1);

        assertThat(codeWithSuffix).isNotEqualTo(code);
        assertThat(codeWithSuffix).hasSize(7);
    }
}
