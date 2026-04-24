// src/main/java/com/bingeboxed/reviews/dto/ReviewStatsResponse.java
package com.bingeboxed.reviews.dto;

public class ReviewStatsResponse {

    private long totalReviews;
    private double averageRating;
    private long fiveStarCount;
    private long fourStarCount;
    private long threeStarCount;
    private long twoStarCount;
    private long oneStarCount;

    public long getTotalReviews() { return totalReviews; }
    public void setTotalReviews(long totalReviews) { this.totalReviews = totalReviews; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getFiveStarCount() { return fiveStarCount; }
    public void setFiveStarCount(long fiveStarCount) { this.fiveStarCount = fiveStarCount; }

    public long getFourStarCount() { return fourStarCount; }
    public void setFourStarCount(long fourStarCount) { this.fourStarCount = fourStarCount; }

    public long getThreeStarCount() { return threeStarCount; }
    public void setThreeStarCount(long threeStarCount) { this.threeStarCount = threeStarCount; }

    public long getTwoStarCount() { return twoStarCount; }
    public void setTwoStarCount(long twoStarCount) { this.twoStarCount = twoStarCount; }

    public long getOneStarCount() { return oneStarCount; }
    public void setOneStarCount(long oneStarCount) { this.oneStarCount = oneStarCount; }
}