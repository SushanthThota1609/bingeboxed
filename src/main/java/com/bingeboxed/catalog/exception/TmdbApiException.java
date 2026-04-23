package com.bingeboxed.catalog.exception;

public class TmdbApiException extends RuntimeException {
    private final int statusCode;

    public TmdbApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}