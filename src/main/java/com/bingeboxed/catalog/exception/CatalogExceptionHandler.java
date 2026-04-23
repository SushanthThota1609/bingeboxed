package com.bingeboxed.catalog.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class CatalogExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CatalogExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TmdbApiException.class)
    public ResponseEntity<Map<String, String>> handleTmdbApi(TmdbApiException e) {
        if (e.getStatusCode() == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Content not found in TMDB"));
        }
        logger.warn("TMDB API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "External API error"));
    }

    @ExceptionHandler(TmdbUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleTmdbUnavailable(TmdbUnavailableException e) {
        logger.error("TMDB unavailable", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "External service unavailable"));
    }
}