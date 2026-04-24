// src/main/java/com/bingeboxed/reviews/dto/ReviewResponse.java
package com.bingeboxed.reviews.dto;

import java.time.LocalDateTime;

public class ReviewResponse {
    private Long id;
    private Integer tmdbId;
    private String contentType;
    private Integer rating;
    private String reviewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserInfo user;
    private ContentInfo content;
    
    public static class UserInfo {
        private Long userId;
        private String displayName;
        
        public UserInfo(Long userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
    
    public static class ContentInfo {
        private Integer tmdbId;
        private String title;
        private String posterPath;
        
        public ContentInfo(Integer tmdbId, String title, String posterPath) {
            this.tmdbId = tmdbId;
            this.title = title;
            this.posterPath = posterPath;
        }
        
        // Getters and Setters
        public Integer getTmdbId() { return tmdbId; }
        public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getPosterPath() { return posterPath; }
        public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }
    
    public ContentInfo getContent() { return content; }
    public void setContent(ContentInfo content) { this.content = content; }
}