// src/main/java/com/bingeboxed/reviews/service/ReviewService.java
package com.bingeboxed.reviews.service;

import com.bingeboxed.reviews.dto.*;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(String email, CreateReviewRequest request);

    ReviewResponse updateReview(String email, Integer tmdbId, UpdateReviewRequest request);

    void deleteReview(String email, Integer tmdbId);

    List<ReviewResponse> getMyReviews(String email, String type, Integer minRating, Integer maxRating);

    ReviewResponse getMyReviewForContent(String email, Integer tmdbId);

    List<ReviewResponse> getReviewsForContent(Integer tmdbId);

    List<ReviewResponse> getPublicReviewsForUser(Long userId, String type, Integer minRating, Integer maxRating);

    ReviewStatsResponse getMyStats(String email);

    ContentRatingResponse getContentRating(Integer tmdbId);

    ContainsReviewResponse containsReview(String email, Integer tmdbId);
}