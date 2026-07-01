package com.example.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AliasAlreadyExistsException extends RuntimeException {

    private final String alias;

    public AliasAlreadyExistsException(String alias) {
        super(String.format("Short code or custom alias '%s' already exists", alias));
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}
