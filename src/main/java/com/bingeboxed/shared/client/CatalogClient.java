package com.bingeboxed.shared.client;

import com.bingeboxed.catalog.dto.ContentResponse;

public interface CatalogClient {
    ContentResponse getContentById(Integer tmdbId, String type);
}