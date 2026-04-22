package com.bingeboxed.catalog.controller;

import com.bingeboxed.catalog.dto.CatalogResult;
import com.bingeboxed.catalog.dto.ContentDto;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PagedResponse;
import com.bingeboxed.catalog.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for catalog operations.
 * All endpoints require a valid JWT (SEC-01 — enforced by SecurityConfig + JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogApiController {

    private final CatalogService catalogService;

    public CatalogApiController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // -------------------------------------------------------------------------
    // FR-01: Fetch content by TMDB ID (cache-first)
    // -------------------------------------------------------------------------

    @GetMapping("/{tmdbId}")
    public ResponseEntity<ContentDto> getContent(
            @PathVariable String tmdbId,
            @RequestParam(required = false) String type) {

        int id = parseTmdbId(tmdbId);
        validateType(type);

        CatalogResult<ContentDto> result = catalogService.fetchContent(id, type);

        if (result.isCacheFallback()) {
            return ResponseEntity.ok()
                    .header("X-Cache-Fallback", "true")
                    .body(result.getData());
        }
        return ResponseEntity.ok(result.getData());
    }

    // -------------------------------------------------------------------------
    // FR-02: Search content
    // -------------------------------------------------------------------------

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ContentDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page) {

        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Query parameter 'q' must not be blank");
        }
        validateType(type);
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be at least 1");
        }

        return ResponseEntity.ok(catalogService.searchContent(q, type, page));
    }

    // -------------------------------------------------------------------------
    // FR-03: Browse content with filters
    // -------------------------------------------------------------------------

    @GetMapping("/browse")
    public ResponseEntity<PagedResponse<ContentDto>> browse(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "1") int page) {

        validateType(type);
        if (year != null) {
            validateYear(year);
        }
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be at least 1");
        }

        return ResponseEntity.ok(catalogService.browseContent(type, genre, year, page));
    }

    // -------------------------------------------------------------------------
    // FR-04: Full detail view (always fresh)
    // -------------------------------------------------------------------------

    @GetMapping("/{tmdbId}/detail")
    public ResponseEntity<ContentDto> getDetail(
            @PathVariable String tmdbId,
            @RequestParam(required = false) String type) {

        int id = parseTmdbId(tmdbId);
        validateType(type);

        return ResponseEntity.ok(catalogService.fetchDetail(id, type));
    }

    // -------------------------------------------------------------------------
    // FR-05: List available genres
    // -------------------------------------------------------------------------

    @GetMapping("/genres")
    public ResponseEntity<List<GenreDto>> getGenres(
            @RequestParam(required = false) String type) {

        validateType(type);
        return ResponseEntity.ok(catalogService.fetchGenres(type));
    }

    // -------------------------------------------------------------------------
    // FR-06: Trending content
    // -------------------------------------------------------------------------

    @GetMapping("/trending")
    public ResponseEntity<List<ContentDto>> getTrending(
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "week") String window) {

        validateType(type);
        validateWindow(window);
        return ResponseEntity.ok(catalogService.fetchTrending(type, window));
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    private int parseTmdbId(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tmdbId is required");
        }
        try {
            int id = Integer.parseInt(raw);
            if (id <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "tmdbId must be a positive integer");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tmdbId must be a valid integer");
        }
    }

    private void validateType(String type) {
        if (type == null || (!type.equalsIgnoreCase("movie") && !type.equalsIgnoreCase("series"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "type must be 'movie' or 'series'");
        }
    }

    private void validateWindow(String window) {
        if (!window.equals("day") && !window.equals("week")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "window must be 'day' or 'week'");
        }
    }

    private void validateYear(int year) {
        int maxYear = LocalDate.now().getYear() + 1;
        if (year < 1888 || year > maxYear) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "year must be between 1888 and " + maxYear);
        }
    }
}