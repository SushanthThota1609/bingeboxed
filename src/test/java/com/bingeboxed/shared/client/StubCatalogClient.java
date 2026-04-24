package com.bingeboxed.shared.client;

import com.bingeboxed.catalog.dto.ContentResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class StubCatalogClient implements CatalogClient {

    @Override
    public ContentResponse getContentById(Integer tmdbId, String type) {
        return new ContentResponse(tmdbId, "Test Title", type, "Test overview", 2024, "Action", "/poster.jpg");
    }
}
