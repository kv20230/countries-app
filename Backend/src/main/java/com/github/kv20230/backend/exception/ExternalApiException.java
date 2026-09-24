package com.github.kv20230.backend.exception;

import org.springframework.http.HttpStatusCode;

public class ExternalApiException extends RuntimeException{

    private final HttpStatusCode statusCode;

    public ExternalApiException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
