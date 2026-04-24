// src/main/java/com/bingeboxed/reviews/dto/UpdateReviewRequest.java
package com.bingeboxed.reviews.dto;

public class UpdateReviewRequest {

    private Integer rating;
    private String reviewText;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
}