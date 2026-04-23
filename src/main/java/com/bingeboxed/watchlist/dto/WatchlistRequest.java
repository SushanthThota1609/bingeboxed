package com.bingeboxed.watchlist.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WatchlistRequest {

    @NotNull(message = "tmdbId is required")
    @Min(value = 1, message = "tmdbId must be positive")
    private Integer tmdbId;

    @NotBlank(message = "contentType is required")
    private String contentType;

    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}