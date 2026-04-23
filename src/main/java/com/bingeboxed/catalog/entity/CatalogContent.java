package com.bingeboxed.catalog.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_content")
public class CatalogContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", unique = true, nullable = false)
    private Integer tmdbId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content_type", nullable = false, length = 10)
    private String contentType;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "genre")
    private String genre;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "cached_at")
    private LocalDateTime cachedAt;

    public CatalogContent() {
    }

    public CatalogContent(Integer tmdbId, String title, String contentType, String overview,
                          Integer releaseYear, String genre, String posterUrl, LocalDateTime cachedAt) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.contentType = contentType;
        this.overview = overview;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.posterUrl = posterUrl;
        this.cachedAt = cachedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public LocalDateTime getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(LocalDateTime cachedAt) {
        this.cachedAt = cachedAt;
    }
}