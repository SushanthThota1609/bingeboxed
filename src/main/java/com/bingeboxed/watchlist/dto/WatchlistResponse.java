package com.bingeboxed.watchlist.dto;

import com.bingeboxed.catalog.dto.ContentResponse;
import java.time.Instant;

public class WatchlistResponse {

    private Long id;
    private Integer tmdbId;
    private String contentType;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private ContentResponse contentMetadata;

    public WatchlistResponse() {}

    public WatchlistResponse(Long id, Integer tmdbId, String contentType, String status,
                             Instant createdAt, Instant updatedAt, ContentResponse contentMetadata) {
        this.id = id;
        this.tmdbId = tmdbId;
        this.contentType = contentType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.contentMetadata = contentMetadata;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public ContentResponse getContentMetadata() { return contentMetadata; }
    public void setContentMetadata(ContentResponse contentMetadata) { this.contentMetadata = contentMetadata; }
}