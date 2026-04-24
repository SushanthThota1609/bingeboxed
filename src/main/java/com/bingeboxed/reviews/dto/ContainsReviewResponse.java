// src/main/java/com/bingeboxed/reviews/dto/ContainsReviewResponse.java
package com.bingeboxed.reviews.dto;

public class ContainsReviewResponse {

    private boolean hasReviewed;
    private Integer rating;

    public ContainsReviewResponse(boolean hasReviewed, Integer rating) {
        this.hasReviewed = hasReviewed;
        this.rating = rating;
    }

    public boolean isHasReviewed() { return hasReviewed; }
    public void setHasReviewed(boolean hasReviewed) { this.hasReviewed = hasReviewed; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}