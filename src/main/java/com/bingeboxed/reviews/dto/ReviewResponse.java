// src/main/java/com/bingeboxed/reviews/dto/ReviewResponse.java
package com.bingeboxed.reviews.dto;

import java.time.LocalDateTime;

public class ReviewResponse {

    private Long id;
    private Long userId;
    private String displayName;
    private Integer tmdbId;
    private String contentType;
    private Integer rating;
    private String reviewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Enriched content metadata
    private String contentTitle;
    private String contentPosterUrl;
    private Integer contentReleaseYear;
    private String contentGenre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getContentTitle() { return contentTitle; }
    public void setContentTitle(String contentTitle) { this.contentTitle = contentTitle; }

    public String getContentPosterUrl() { return contentPosterUrl; }
    public void setContentPosterUrl(String contentPosterUrl) { this.contentPosterUrl = contentPosterUrl; }

    public Integer getContentReleaseYear() { return contentReleaseYear; }
    public void setContentReleaseYear(Integer contentReleaseYear) { this.contentReleaseYear = contentReleaseYear; }

    public String getContentGenre() { return contentGenre; }
    public void setContentGenre(String contentGenre) { this.contentGenre = contentGenre; }
}