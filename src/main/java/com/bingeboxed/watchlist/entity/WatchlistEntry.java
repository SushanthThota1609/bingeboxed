// src/main/java/com/bingeboxed/watchlist/entity/WatchlistEntry.java
package com.bingeboxed.watchlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "watchlist_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_watchlist_user_tmdb",
                columnNames = {"user_id", "tmdb_id"}
        )
)
public class WatchlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tmdb_id", nullable = false)
    private Integer tmdbId;

    @Column(name = "content_type", nullable = false, length = 10)
    private String contentType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WatchlistEntry() {}

    public Long        getId()          { return id; }
    public Long        getUserId()      { return userId; }
    public Integer     getTmdbId()      { return tmdbId; }
    public String      getContentType() { return contentType; }
    public String      getStatus()      { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id)                   { this.id = id; }
    public void setUserId(Long userId)           { this.userId = userId; }
    public void setTmdbId(Integer tmdbId)        { this.tmdbId = tmdbId; }
    public void setContentType(String ct)        { this.contentType = ct; }
    public void setStatus(String status)         { this.status = status; }
    public void setCreatedAt(LocalDateTime t)    { this.createdAt = t; }
    public void setUpdatedAt(LocalDateTime t)    { this.updatedAt = t; }
}