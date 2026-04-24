// src/main/java/com/bingeboxed/reviews/dto/HasReviewedResponse.java
package com.bingeboxed.reviews.dto;

public class HasReviewedResponse {
    private boolean hasReviewed;
    private Integer rating;
    
    public HasReviewedResponse(boolean hasReviewed, Integer rating) {
        this.hasReviewed = hasReviewed;
        this.rating = rating;
    }
    
    // Getters and Setters
    public boolean isHasReviewed() { return hasReviewed; }
    public void setHasReviewed(boolean hasReviewed) { this.hasReviewed = hasReviewed; }
    
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}