package com.bingeboxed.catalog.client;

import com.bingeboxed.catalog.dto.ContentDto;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PagedResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private final String apiKey;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TmdbClient(
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${tmdb.base.url}") String baseUrl,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // -------------------------------------------------------------------------
    // Primary public API
    // -------------------------------------------------------------------------

    /**
     * Fetches a single content item. Throws TmdbApiException for non-404 errors,
     * TmdbUnavailableException when the API cannot be reached.
     */
    public ContentDto fetchContentById(int tmdbId, String type) {
        return fetchById(tmdbId, type)
                .orElseThrow(() -> new TmdbApiException(404, "Not found on TMDB: " + tmdbId));
    }

    /**
     * Returns Optional.empty() on 404; throws TmdbApiException for other non-2xx
     * responses; throws TmdbUnavailableException when the host is unreachable.
     * Tests mock this method directly.
     */
    public Optional<ContentDto> fetchById(int tmdbId, String type) {
        String url = baseUrl + "/" + type + "/" + tmdbId + "?api_key=" + apiKey;
        try {
            JsonNode node = executeGet(url);
            return Optional.of(buildContentDto(node, type));
        } catch (TmdbApiException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public PagedResponseDto<ContentDto> searchContent(String query, String type, int page) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = baseUrl + "/search/" + type
                + "?api_key=" + apiKey
                + "&query=" + encodedQuery
                + "&page=" + page;
        JsonNode node = executeGet(url);
        return buildPagedResponse(node, type);
    }

    /** Alias expected by tests. */
    public PagedResponseDto<ContentDto> search(String query, String type, int page) {
        return searchContent(query, type, page);
    }

    public PagedResponseDto<ContentDto> discoverContent(String type, String genre, Integer year, int page) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl)
                .append("/discover/").append(type)
                .append("?api_key=").append(apiKey)
                .append("&page=").append(page);

        if (genre != null && !genre.isBlank()) {
            urlBuilder.append("&with_genres=")
                    .append(URLEncoder.encode(genre.trim(), StandardCharsets.UTF_8));
        }
        if (year != null) {
            String yearParam = "tv".equals(type) ? "first_air_date_year" : "primary_release_year";
            urlBuilder.append("&").append(yearParam).append("=").append(year);
        }

        JsonNode node = executeGet(urlBuilder.toString());
        return buildPagedResponse(node, type);
    }

    /** Alias expected by tests. */
    public PagedResponseDto<ContentDto> discover(String type, String genre, Integer year, int page) {
        return discoverContent(type, genre, year, page);
    }

    public List<GenreDto> fetchGenres(String type) {
        String url = baseUrl + "/genre/" + type + "/list?api_key=" + apiKey;
        JsonNode node = executeGet(url);
        List<GenreDto> genres = new ArrayList<>();
        JsonNode genresArray = node.path("genres");
        if (genresArray.isArray()) {
            for (JsonNode g : genresArray) {
                genres.add(new GenreDto(g.path("id").asInt(), g.path("name").asText("")));
            }
        }
        return genres;
    }

    /** Alias expected by tests. */
    public List<GenreDto> getGenres(String type) {
        return fetchGenres(type);
    }

    public List<ContentDto> fetchTrending(String type, String window) {
        String url = baseUrl + "/trending/" + type + "/" + window + "?api_key=" + apiKey;
        JsonNode node = executeGet(url);
        List<ContentDto> results = new ArrayList<>();
        JsonNode resultsArray = node.path("results");
        if (resultsArray.isArray()) {
            for (JsonNode item : resultsArray) {
                results.add(buildContentDto(item, type));
            }
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private JsonNode executeGet(String url) {
        String logUrl = url.replaceAll("api_key=[^&]+", "api_key=***");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                log.warn("TMDB API returned non-2xx status {} for: {}", status, logUrl);
                throw new TmdbApiException(status, "TMDB returned HTTP " + status);
            }

            return objectMapper.readTree(response.body());

        } catch (TmdbApiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("TMDB HTTP request interrupted for: {}", logUrl);
            throw new TmdbUnavailableException("TMDB request interrupted", e);
        } catch (IOException e) {
            log.warn("TMDB API unreachable for: {} — {}", logUrl, e.getMessage());
            throw new TmdbUnavailableException("TMDB API unreachable", e);
        }
    }

    private PagedResponseDto<ContentDto> buildPagedResponse(JsonNode node, String type) {
        List<ContentDto> results = new ArrayList<>();
        JsonNode resultsArray = node.path("results");
        if (resultsArray.isArray()) {
            for (JsonNode item : resultsArray) {
                results.add(buildContentDto(item, type));
            }
        }
        int page = node.path("page").asInt(1);
        int totalPages = node.path("total_pages").asInt(0);
        int totalResults = node.path("total_results").asInt(0);
        return new PagedResponseDto<>(results, page, totalPages, totalResults);
    }

    private ContentDto buildContentDto(JsonNode node, String type) {
        boolean isTv = "tv".equals(type);

        String title = isTv
                ? node.path("name").asText(node.path("title").asText(null))
                : node.path("title").asText(null);

        String dateField = isTv ? "first_air_date" : "release_date";
        String date = node.path(dateField).asText(null);
        Integer releaseYear = parseYear(date);

        String posterPath = node.path("poster_path").asText(null);
        String posterUrl = (posterPath != null && !posterPath.isBlank())
                ? IMAGE_BASE_URL + posterPath
                : null;

        String genre = extractGenre(node);
        String contentType = isTv ? "SERIES" : "MOVIE";

        ContentDto dto = new ContentDto();
        dto.setTmdbId(node.path("id").asInt());
        dto.setTitle(title);
        dto.setContentType(contentType);
        dto.setOverview(node.path("overview").asText(null));
        dto.setReleaseYear(releaseYear);
        dto.setGenre(genre);
        dto.setPosterUrl(posterUrl);
        return dto;
    }

    private Integer parseYear(String date) {
        if (date == null || date.length() < 4) return null;
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractGenre(JsonNode node) {
        JsonNode genresNode = node.path("genres");
        if (genresNode.isArray() && genresNode.size() > 0) {
            List<String> names = new ArrayList<>();
            for (JsonNode g : genresNode) {
                String name = g.path("name").asText(null);
                if (name != null && !name.isBlank()) names.add(name);
            }
            if (!names.isEmpty()) return String.join(", ", names);
        }

        JsonNode genreIds = node.path("genre_ids");
        if (genreIds.isArray() && genreIds.size() > 0) {
            List<String> ids = new ArrayList<>();
            for (JsonNode g : genreIds) ids.add(String.valueOf(g.asInt()));
            if (!ids.isEmpty()) return String.join(", ", ids);
        }

        return null;
    }
}