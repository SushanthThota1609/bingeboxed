package com.bingeboxed.catalog.dto;

public class CatalogResult<T> {

    private final T data;
    private final boolean cacheFallback;

    public CatalogResult(T data, boolean cacheFallback) {
        this.data = data;
        this.cacheFallback = cacheFallback;
    }

    public T getData() { return data; }
    public boolean isCacheFallback() { return cacheFallback; }
}