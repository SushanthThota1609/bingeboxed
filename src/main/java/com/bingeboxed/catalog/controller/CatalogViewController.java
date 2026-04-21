package com.bingeboxed.catalog.controller;

import com.bingeboxed.catalog.dto.ContentDto;
import com.bingeboxed.catalog.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * MVC controller for catalog Thymeleaf views.
 * Authentication is enforced by SecurityConfig for /catalog/** routes.
 */
@Controller
public class CatalogViewController {

    private final CatalogService catalogService;

    public CatalogViewController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * FR-07: Catalog browse page.
     */
    @GetMapping("/catalog")
    public String browsePage() {
        return "catalog/browse";
    }

    /**
     * FR-08: Content detail page.
     */
    @GetMapping("/catalog/{tmdbId}")
    public String detailPage(
            @PathVariable String tmdbId,
            @RequestParam(required = false) String type,
            Model model) {

        int id = parseTmdbId(tmdbId);
        validateType(type);

        ContentDto content = catalogService.fetchContent(id, type).getData();
        model.addAttribute("content", content);
        model.addAttribute("type", type);
        model.addAttribute("tmdbId", id);

        return "catalog/detail";
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
        if (type == null || (!type.equals("movie") && !type.equals("series"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "type must be 'movie' or 'series'");
        }
    }
}