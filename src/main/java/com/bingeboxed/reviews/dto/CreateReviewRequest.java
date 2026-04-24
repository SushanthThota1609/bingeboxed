// src/main/java/com/bingeboxed/reviews/dto/CreateReviewRequest.java
package com.bingeboxed.reviews.dto;

public class CreateReviewRequest {

    private Integer tmdbId;
    private String contentType;
    private Integer rating;
    private String reviewText;

    public Integer getTmdbId() { return tmdbId; }
    public void setTmdbId(Integer tmdbId) { this.tmdbId = tmdbId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
}