package com.bingeboxed.recommendations.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecommendationResponse {

    private Long id;
    private Integer tmdbId;
    private String title;
    private String contentType;
    private String overview;
    private Integer releaseYear;
    private String genre;
    private String posterUrl;
    private BigDecimal score;
    private String reason;
    private LocalDateTime generatedAt;

    public RecommendationResponse() {}

    public RecommendationResponse(Long id, Integer tmdbId, String title, String contentType,
                                  String overview, Integer releaseYear, String genre,
                                  String posterUrl, BigDecimal score, String reason,
                                  LocalDateTime generatedAt) {
        this.id = id;
        this.tmdbId = tmdbId;
        this.title = title;
        this.contentType = contentType;
        this.overview = overview;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.posterUrl = posterUrl;
        this.score = score;
        this.reason = reason;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
