package com.bingeboxed.social.dto;

public class PublicWatchlistEntryDto {
    private Long contentId;
    private String title;
    private String posterPath;
    private String status;
    private Integer rating;
    private String review;

    public PublicWatchlistEntryDto(Long contentId, String title, String posterPath, String status, Integer rating, String review) {
        this.contentId = contentId;
        this.title = title;
        this.posterPath = posterPath;
        this.status = status;
        this.rating = rating;
        this.review = review;
    }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
}
