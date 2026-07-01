package com.example.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUrlException extends RuntimeException {

    private final String url;

    public InvalidUrlException(String url) {
        super(String.format("The provided URL is not valid: '%s'", url));
        this.url = url;
    }

    public InvalidUrlException(String url, String reason) {
        super(String.format("The provided URL '%s' is not valid: %s", url, reason));
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
