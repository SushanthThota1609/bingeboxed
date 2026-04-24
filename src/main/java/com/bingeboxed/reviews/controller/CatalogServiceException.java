// src/main/java/com/bingeboxed/shared/exception/CatalogServiceException.java
package com.bingeboxed.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CatalogServiceException extends RuntimeException {
    public CatalogServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Add this handler to GlobalExceptionHandler