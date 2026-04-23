// src/main/java/com/bingeboxed/shared/client/CatalogClientException.java
package com.bingeboxed.shared.client;

public class CatalogClientException extends RuntimeException {

    private final int statusCode;

    public CatalogClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public CatalogClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 503;
    }

    public int getStatusCode() { return statusCode; }
}