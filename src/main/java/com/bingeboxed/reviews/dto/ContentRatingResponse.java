// src/main/java/com/bingeboxed/reviews/dto/ContentRatingResponse.java
package com.bingeboxed.reviews.dto;

public class ContentRatingResponse {

    private double averageRating;
    private long reviewCount;

    public ContentRatingResponse(double averageRating, long reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
}