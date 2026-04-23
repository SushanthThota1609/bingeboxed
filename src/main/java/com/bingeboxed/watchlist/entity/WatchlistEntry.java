package com.bingeboxed.watchlist.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "watchlist_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "tmdb_id"})
})
public class WatchlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tmdb_id", nullable = false)
    private Integer tmdbId;

    @Column(name = "content_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WatchlistStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum ContentType {
        MOVIE, SERIES
    }

    public enum WatchlistStatus {
        WANT_TO_WATCH, WATCHING, COMPLETED
    }

    // Constructors
    public WatchlistEntry() {}

    public WatchlistEntry(Long userId, Integer tmdbId, ContentType contentType, WatchlistStatus status) {
        this.userId = userId;
        this.tmdbId = tmdbId;
        this.contentType = contentType;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public WatchlistStatus getStatus() { return status; }
    public void setStatus(WatchlistStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}