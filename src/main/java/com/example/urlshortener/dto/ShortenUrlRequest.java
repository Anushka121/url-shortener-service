package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenUrlRequest {

    @NotBlank(message = "Original URL must not be blank")
    @Pattern(
        regexp = "^(https?://)[\\w\\-]+(\\.[\\w\\-]+)+([\\w.,@?^=%&:/~+#\\-_]*[\\w@?^=%&/~+#\\-_])?$",
        message = "Invalid URL format. Must start with http:// or https://"
    )
    private String originalUrl;

    @Size(min = 4, max = 30, message = "Custom alias must be between 4 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]*$",
        message = "Custom alias can only contain alphanumeric characters, hyphens and underscores"
    )
    private String customAlias;
}
