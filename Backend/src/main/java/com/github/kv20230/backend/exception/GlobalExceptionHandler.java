package com.github.kv20230.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ProblemDetail> handleExternalApiException(ExternalApiException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage()
        );
        problemDetail.setTitle("Upstream API Error");
        problemDetail.setType(URI.create("https://api.restcountries.com/errors/upstream-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("upstreamStatusCode", ex.getStatusCode().value());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problemDetail);
    }

    /**
     * Keeps the status the service chose (404 for an unknown country, 400 for a bad page).
     * Without this the generic handler below would turn every one of them into a 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getReason());
        HttpStatus resolved = HttpStatus.resolve(status.value());
        problemDetail.setTitle(resolved != null ? resolved.getReasonPhrase() : "Request failed");
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred on the backend server."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
