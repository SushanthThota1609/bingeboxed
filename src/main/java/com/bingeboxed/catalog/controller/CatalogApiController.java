package com.bingeboxed.catalog.controller;

import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PaginatedResponse;
import com.bingeboxed.catalog.dto.TrendingItemDto;
import com.bingeboxed.catalog.exception.TmdbApiException;
import com.bingeboxed.catalog.exception.TmdbUnavailableException;
import com.bingeboxed.catalog.service.CatalogService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogApiController {

    private static final Logger logger = LoggerFactory.getLogger(CatalogApiController.class);
    private final CatalogService catalogService;

    public CatalogApiController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{tmdbId}")
    public ResponseEntity<?> getContent(@PathVariable Integer tmdbId,
                                        @RequestParam String type,
                                        HttpServletResponse response) {
        validateTmdbId(tmdbId);
        validateType(type);

        try {
            ContentResponse content = catalogService.getContentByTmdbId(tmdbId, type, false);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(content);
        } catch (TmdbUnavailableException e) {
            // If we got here, service already tried fallback and failed
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ContentResponse>> search(
            @RequestParam String q,
            @RequestParam String type,
            @RequestParam(defaultValue = "1") int page) {

        if (q == null || q.trim().isBlank()) {
            throw new IllegalArgumentException("Query parameter 'q' is required and cannot be blank");
        }
        validateType(type);
        if (page < 1) {
            throw new IllegalArgumentException("Page must be at least 1");
        }
        String trimmedQuery = q.trim();
        PaginatedResponse<ContentResponse> results = catalogService.searchContent(trimmedQuery, type, page);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/browse")
    public ResponseEntity<PaginatedResponse<ContentResponse>> browse(
            @RequestParam String type,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "1") int page) {

        validateType(type);
        if (page < 1) {
            throw new IllegalArgumentException("Page must be at least 1");
        }
        if (year != null) {
            int currentYear = Year.now().getValue();
            if (year < 1888 || year > currentYear + 1) {
                throw new IllegalArgumentException("Year must be between 1888 and " + (currentYear + 1));
            }
        }

        PaginatedResponse<ContentResponse> results = catalogService.browseContent(type, genre, year, page);
        // Check if page exceeds total pages
        if (results.getTotalPages() > 0 && page > results.getTotalPages()) {
            throw new IllegalArgumentException("Page exceeds total available pages");
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{tmdbId}/detail")
    public ResponseEntity<ContentResponse> getFullDetail(@PathVariable Integer tmdbId,
                                                         @RequestParam String type) {
        validateTmdbId(tmdbId);
        validateType(type);

        ContentResponse content = catalogService.getFullDetail(tmdbId, type);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(content);
    }

    @GetMapping("/genres")
    public ResponseEntity<List<GenreDto>> getGenres(@RequestParam String type) {
        validateType(type);
        List<GenreDto> genres = catalogService.getGenres(type);
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<TrendingItemDto>> getTrending(
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "week") String window) {

        validateType(type);
        if (!"day".equals(window) && !"week".equals(window)) {
            throw new IllegalArgumentException("Window must be 'day' or 'week'");
        }
        List<TrendingItemDto> trending = catalogService.getTrending(type, window);
        return ResponseEntity.ok(trending);
    }

    private void validateType(String type) {
        if (type == null || (!"movie".equals(type) && !"series".equals(type))) {
            throw new IllegalArgumentException("Type must be 'movie' or 'series'");
        }
    }

    private void validateTmdbId(Integer tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new IllegalArgumentException("tmdbId must be a positive integer");
        }
    }
}