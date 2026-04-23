// src/main/java/com/bingeboxed/shared/client/CatalogClient.java
package com.bingeboxed.shared.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Internal HTTP client for the catalog service.
 * Calls GET /api/catalog/{tmdbId}?type={type} on the same host.
 * Uses Java 11 HttpClient with a 5-second connect and 10-second read timeout (NFR-03).
 */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final String       baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient   httpClient;

    public CatalogClient(
            @Value("${catalog.internal.base.url}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl      = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Fetches content metadata by TMDB ID and type.
     *
     * @param tmdbId      the TMDB content ID
     * @param contentType "MOVIE" or "SERIES" (internal enum value)
     * @return Optional.empty() if the catalog returns 404
     * @throws CatalogUnavailableException if the catalog host is unreachable
     * @throws CatalogClientException      for any other non-2xx response
     */
    public Optional<CatalogContentDto> findById(int tmdbId, String contentType) {
        // Catalog controller expects lowercase "movie" or "series"
        String typeParam = "SERIES".equalsIgnoreCase(contentType) ? "series" : "movie";
        String url = baseUrl + "/api/catalog/" + tmdbId + "?type=" + typeParam;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();

            if (status == 404) {
                return Optional.empty();
            }

            if (status < 200 || status >= 300) {
                log.warn("CatalogClient received non-2xx {} for tmdbId={}", status, tmdbId);
                throw new CatalogClientException(status,
                        "Catalog service returned HTTP " + status);
            }

            CatalogContentDto dto =
                    objectMapper.readValue(response.body(), CatalogContentDto.class);
            return Optional.of(dto);

        } catch (CatalogClientException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("CatalogClient request interrupted for tmdbId={}", tmdbId);
            throw new CatalogUnavailableException("Catalog request interrupted", e);
        } catch (IOException e) {
            log.warn("CatalogClient unreachable for tmdbId={} — {}", tmdbId, e.getMessage());
            throw new CatalogUnavailableException("Catalog service unreachable", e);
        }
    }
}