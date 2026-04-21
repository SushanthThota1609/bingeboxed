package com.bingeboxed.catalog.client;

public class TmdbUnavailableException extends RuntimeException {

    public TmdbUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}