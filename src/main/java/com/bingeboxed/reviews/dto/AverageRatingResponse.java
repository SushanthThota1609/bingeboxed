// src/main/java/com/bingeboxed/reviews/dto/AverageRatingResponse.java
package com.bingeboxed.reviews.dto;

public class AverageRatingResponse {
    private double averageRating;
    private long reviewCount;
    
    public AverageRatingResponse(double averageRating, long reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }
    
    // Getters and Setters
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
}