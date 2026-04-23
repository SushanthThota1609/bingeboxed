// src/main/java/com/bingeboxed/shared/client/CatalogUnavailableException.java
package com.bingeboxed.shared.client;

public class CatalogUnavailableException extends CatalogClientException {

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}