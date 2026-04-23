package com.bingeboxed.catalog.client;

import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PaginatedResponse;
import com.bingeboxed.catalog.dto.TrendingItemDto;
import com.bingeboxed.catalog.exception.TmdbApiException;
import com.bingeboxed.catalog.exception.TmdbUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class TmdbClient {

    private static final Logger logger = LoggerFactory.getLogger(TmdbClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${tmdb.base.url}")
    private String baseUrl;

    @Value("${tmdb.api.key}")
    private String apiKey;

    public TmdbClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private HttpRequest.Builder requestBuilder(String path) {
        String sep = path.contains("?") ? "&" : "?";
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path + sep + "api_key=" + apiKey))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
    }

    private JsonNode getJsonResponse(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) {
                return objectMapper.readTree(response.body());
            } else {
                logger.warn("TMDB API error: status {}", status);
                throw new TmdbApiException("TMDB returned status " + status, status);
            }
        } catch (TmdbApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("TMDB unreachable", e);
            throw new TmdbUnavailableException("Failed to connect to TMDB", e);
        }
    }

    public ContentResponse fetchMovieById(Integer tmdbId) {
        HttpRequest request = requestBuilder("/movie/" + tmdbId).GET().build();
        JsonNode root = getJsonResponse(request);
        return mapMovieResponse(root);
    }

    public ContentResponse fetchSeriesById(Integer tmdbId) {
        HttpRequest request = requestBuilder("/tv/" + tmdbId).GET().build();
        JsonNode root = getJsonResponse(request);
        return mapSeriesResponse(root);
    }

    public PaginatedResponse<ContentResponse> searchMovies(String query, int page) {
        String encoded = URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest request = requestBuilder("/search/movie?query=" + encoded + "&page=" + page).GET().build();
        JsonNode root = getJsonResponse(request);
        return mapSearchResults(root, "movie");
    }

    public PaginatedResponse<ContentResponse> searchSeries(String query, int page) {
        String encoded = URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest request = requestBuilder("/search/tv?query=" + encoded + "&page=" + page).GET().build();
        JsonNode root = getJsonResponse(request);
        return mapSearchResults(root, "series");
    }

    public PaginatedResponse<ContentResponse> discoverMovies(Integer year, String genreId, int page) {
        StringBuilder url = new StringBuilder("/discover/movie?page=" + page);
        if (year != null) url.append("&primary_release_year=").append(year);
        if (genreId != null) url.append("&with_genres=").append(genreId);
        HttpRequest request = requestBuilder(url.toString()).GET().build();
        JsonNode root = getJsonResponse(request);
        return mapSearchResults(root, "movie");
    }

    public PaginatedResponse<ContentResponse> discoverSeries(Integer year, String genreId, int page) {
        StringBuilder url = new StringBuilder("/discover/tv?page=" + page);
        if (year != null) url.append("&first_air_date_year=").append(year);
        if (genreId != null) url.append("&with_genres=").append(genreId);
        HttpRequest request = requestBuilder(url.toString()).GET().build();
        JsonNode root = getJsonResponse(request);
        return mapSearchResults(root, "series");
    }

    public List<GenreDto> getMovieGenres() {
        HttpRequest request = requestBuilder("/genre/movie/list").GET().build();
        JsonNode root = getJsonResponse(request);
        return mapGenres(root);
    }

    public List<GenreDto> getSeriesGenres() {
        HttpRequest request = requestBuilder("/genre/tv/list").GET().build();
        JsonNode root = getJsonResponse(request);
        return mapGenres(root);
    }

    public List<TrendingItemDto> getTrending(String type, String window) {
        String tmdbType = "movie".equals(type) ? "movie" : "tv";
        HttpRequest request = requestBuilder("/trending/" + tmdbType + "/" + window).GET().build();
        JsonNode root = getJsonResponse(request);
        List<TrendingItemDto> results = new ArrayList<>();
        for (JsonNode item : root.path("results")) {
            boolean isMovie = "movie".equals(type);
            Integer tmdbId = item.path("id").asInt();
            String title = isMovie ? item.path("title").asText() : item.path("name").asText();
            String posterPath = item.path("poster_path").asText();
            String posterUrl = posterPath != null && !posterPath.isBlank() ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
            String releaseDate = isMovie ? item.path("release_date").asText() : item.path("first_air_date").asText();
            Integer year = null;
            if (releaseDate != null && releaseDate.length() >= 4) {
                try { year = Integer.parseInt(releaseDate.substring(0,4)); } catch (NumberFormatException ignored) {}
            }
            String genreNames = null; // Not provided in trending list, left null
            results.add(new TrendingItemDto(tmdbId, title, type, posterUrl, year, genreNames));
        }
        return results;
    }

    private ContentResponse mapMovieResponse(JsonNode node) {
        Integer tmdbId = node.path("id").asInt();
        String title = node.path("title").asText();
        String overview = node.path("overview").asText();
        String releaseDate = node.path("release_date").asText();
        Integer year = null;
        if (releaseDate != null && releaseDate.length() >= 4) {
            try { year = Integer.parseInt(releaseDate.substring(0,4)); } catch (NumberFormatException ignored) {}
        }
        String genre = extractGenreNames(node.path("genres"));
        String posterPath = node.path("poster_path").asText();
        String posterUrl = posterPath != null && !posterPath.isBlank() ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
        return new ContentResponse(tmdbId, title, "MOVIE", overview, year, genre, posterUrl);
    }

    private ContentResponse mapSeriesResponse(JsonNode node) {
        Integer tmdbId = node.path("id").asInt();
        String title = node.path("name").asText();
        String overview = node.path("overview").asText();
        String firstAirDate = node.path("first_air_date").asText();
        Integer year = null;
        if (firstAirDate != null && firstAirDate.length() >= 4) {
            try { year = Integer.parseInt(firstAirDate.substring(0,4)); } catch (NumberFormatException ignored) {}
        }
        String genre = extractGenreNames(node.path("genres"));
        String posterPath = node.path("poster_path").asText();
        String posterUrl = posterPath != null && !posterPath.isBlank() ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
        return new ContentResponse(tmdbId, title, "SERIES", overview, year, genre, posterUrl);
    }

    private PaginatedResponse<ContentResponse> mapSearchResults(JsonNode root, String type) {
        List<ContentResponse> items = new ArrayList<>();
        for (JsonNode result : root.path("results")) {
            Integer tmdbId = result.path("id").asInt();
            String title = "movie".equals(type) ? result.path("title").asText() : result.path("name").asText();
            String overview = result.path("overview").asText();
            String dateField = "movie".equals(type) ? "release_date" : "first_air_date";
            String dateStr = result.path(dateField).asText();
            Integer year = null;
            if (dateStr != null && dateStr.length() >= 4) {
                try { year = Integer.parseInt(dateStr.substring(0,4)); } catch (NumberFormatException ignored) {}
            }
            String posterPath = result.path("poster_path").asText();
            String posterUrl = posterPath != null && !posterPath.isBlank() ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
            // Genre not included in search results; set null
            items.add(new ContentResponse(tmdbId, title, type.toUpperCase(), overview, year, null, posterUrl));
        }
        int page = root.path("page").asInt();
        int totalPages = root.path("total_pages").asInt();
        long totalResults = root.path("total_results").asLong();
        return new PaginatedResponse<>(items, page, totalPages, totalResults);
    }

    private List<GenreDto> mapGenres(JsonNode root) {
        List<GenreDto> genres = new ArrayList<>();
        for (JsonNode g : root.path("genres")) {
            genres.add(new GenreDto(g.path("id").asInt(), g.path("name").asText()));
        }
        return genres;
    }

    private String extractGenreNames(JsonNode genresArray) {
        if (genresArray == null || !genresArray.isArray()) return null;
        List<String> names = new ArrayList<>();
        for (JsonNode g : genresArray) {
            names.add(g.path("name").asText());
        }
        return String.join(", ", names);
    }
}