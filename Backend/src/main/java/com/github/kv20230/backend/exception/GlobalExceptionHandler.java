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

    /**
     * Handles custom {@link ExternalApiException} thrown when a downstream call to an external
     * provider (e.g., restcountries.com) fails. Maps the error to a 502 Bad Gateway response
     * and includes the original upstream HTTP status code in the response payload.
     *
     * @param ex the exception containing the upstream error message and status code
     * @return a {@link ResponseEntity} containing a standardized {@link ProblemDetail} with a 502 status
     */
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
     * Handles Spring's {@link ResponseStatusException}, preserving the specific HTTP status code
     * and reason chosen by the throwing service (e.g., 404 for an unknown country, 400 for a bad page request).
     * Transforms the exception into a standardized RFC 7807 {@link ProblemDetail} format.
     *
     * @param ex the exception containing the target HTTP status code and specific error reason
     * @return a {@link ResponseEntity} containing a {@link ProblemDetail} matching the exception's status code
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

    /**
     * Acts as a global catch-all fallback for any unexpected and unhandled exceptions.
     * Masks the underlying exception stack trace from the client for security reasons,
     * returning a generic 500 Internal Server Error response.
     *
     * @param ex the unexpected exception that bypassed specific handlers
     * @return a {@link ResponseEntity} containing a generic 500 {@link ProblemDetail}
     */
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
