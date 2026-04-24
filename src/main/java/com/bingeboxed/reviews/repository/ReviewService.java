// src/main/java/com/bingeboxed/reviews/service/ReviewService.java
package com.bingeboxed.reviews.service;

import com.bingeboxed.reviews.dto.*;
import com.bingeboxed.reviews.entity.Review;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(Long userId, CreateReviewRequest request);
    ReviewResponse updateReview(Long userId, Integer tmdbId, UpdateReviewRequest request);
    void deleteReview(Long userId, Integer tmdbId);
    List<ReviewResponse> getUserReviews(Long userId, String contentType, Integer minRating, Integer maxRating);
    ReviewResponse getUserReviewByTmdbId(Long userId, Integer tmdbId);
    List<ReviewResponse> getContentReviews(Integer tmdbId);
    List<ReviewResponse> getUserPublicReviews(Long userId, String contentType, Integer minRating, Integer maxRating);
    ReviewStatsResponse getUserStats(Long userId);
    AverageRatingResponse getContentAverageRating(Integer tmdbId);
    HasReviewedResponse checkUserReviewedContent(Long userId, Integer tmdbId);
}