package com.bingeboxed.catalog.exception;

public class TmdbUnavailableException extends RuntimeException {
    public TmdbUnavailableException(String message) {
        super(message);
    }

    public TmdbUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}