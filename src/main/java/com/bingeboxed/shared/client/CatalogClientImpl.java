package com.bingeboxed.shared.client;

import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.catalog.exception.TmdbApiException;
import com.bingeboxed.catalog.exception.TmdbUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CatalogClientImpl implements CatalogClient {

    private final HttpClient httpClient;
    private final String catalogBaseUrl;
    private final ObjectMapper objectMapper;

    public CatalogClientImpl(@Value("${catalog.internal.base.url}") String catalogBaseUrl) {
        this.catalogBaseUrl = catalogBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ContentResponse getContentById(Integer tmdbId, String type) {
        try {
            String url = catalogBaseUrl + "/api/catalog/" + tmdbId + "/detail?type=" + type;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() != 200) {
                throw new TmdbApiException("Catalog service responded with status " + response.statusCode(),
                        response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            ContentResponse content = new ContentResponse();
            content.setTmdbId(root.path("tmdbId").asInt());
            content.setTitle(root.path("title").asText());
            content.setReleaseYear(root.path("releaseYear").asInt());
            content.setPosterUrl(root.path("posterUrl").asText());
            content.setOverview(root.path("overview").asText());
            content.setGenre(root.path("genre").asText());
            return content;
        } catch (java.net.ConnectException | java.net.SocketTimeoutException e) {
            throw new TmdbUnavailableException("Catalog service is unavailable", e);
        } catch (Exception e) {
            throw new TmdbApiException("Failed to fetch content from catalog: " + e.getMessage(), 500);
        }
    }
}