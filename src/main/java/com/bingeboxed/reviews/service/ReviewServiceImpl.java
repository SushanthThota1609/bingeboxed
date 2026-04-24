// src/main/java/com/bingeboxed/reviews/service/ReviewServiceImpl.java
package com.bingeboxed.reviews.service;

import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.reviews.dto.*;
import com.bingeboxed.reviews.entity.Review;
import com.bingeboxed.reviews.repository.ReviewRepository;
import com.bingeboxed.shared.client.CatalogClient;
import com.bingeboxed.shared.exception.CatalogServiceException;
import com.bingeboxed.shared.user.CurrentUserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {
    
    private static final List<String> VALID_CONTENT_TYPES = Arrays.asList("MOVIE", "SERIES");
    
    private final ReviewRepository reviewRepository;
    private final CatalogClient catalogClient;
    private final CurrentUserService currentUserService;
    
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             CatalogClient catalogClient,
                             CurrentUserService currentUserService) {
        this.reviewRepository = reviewRepository;
        this.catalogClient = catalogClient;
        this.currentUserService = currentUserService;
    }
    
    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        if (!VALID_CONTENT_TYPES.contains(request.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "contentType must be MOVIE or SERIES");
        }
        
        boolean contentExists = false;
        try {
            ContentResponse content = catalogClient.getContentById(request.getTmdbId(), request.getContentType());
            if (content != null) {
                contentExists = true;
            }
        } catch (Exception e) {
            System.out.println("Catalog service unavailable, but allowing review creation: " + e.getMessage());
            contentExists = true;
        }
        
        if (!contentExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found in catalog");
        }
        
        if (reviewRepository.existsByUserIdAndTmdbId(userId, request.getTmdbId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this content");
        }
        
        Review review = new Review();
        review.setUserId(userId);
        review.setTmdbId(request.getTmdbId());
        review.setContentType(request.getContentType());
        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());
        
        try {
            Review saved = reviewRepository.save(review);
            return enrichReviewResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this content");
        }
    }
    
    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Integer tmdbId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        
        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());
        
        Review updated = reviewRepository.save(review);
        return enrichReviewResponse(updated);
    }
    
    @Override
    @Transactional
    public void deleteReview(Long userId, Integer tmdbId) {
        Review review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        
        reviewRepository.delete(review);
    }
    
    @Override
    public List<ReviewResponse> getUserReviews(Long userId, String contentType, Integer minRating, Integer maxRating) {
        List<Review> reviews;
        
        if (contentType != null && !contentType.isEmpty()) {
            reviews = reviewRepository.findUserReviewsWithFilters(userId, contentType, minRating, maxRating);
        } else {
            reviews = reviewRepository.findUserReviewsWithRatingFilter(userId, minRating, maxRating);
        }
        
        return reviews.stream()
            .map(this::enrichReviewResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public ReviewResponse getUserReviewByTmdbId(Long userId, Integer tmdbId) {
        Review review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        
        return enrichReviewResponse(review);
    }
    
    @Override
    public List<ReviewResponse> getContentReviews(Integer tmdbId) {
        List<Review> reviews = reviewRepository.findByTmdbIdOrderByCreatedAtDesc(tmdbId);
        
        return reviews.stream()
            .map(this::enrichReviewResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ReviewResponse> getUserPublicReviews(Long userId, String contentType, Integer minRating, Integer maxRating) {
        List<Review> reviews;
        
        if (contentType != null && !contentType.isEmpty()) {
            reviews = reviewRepository.findUserReviewsWithFilters(userId, contentType, minRating, maxRating);
        } else if (minRating != null || maxRating != null) {
            reviews = reviewRepository.findUserReviewsWithRatingFilter(userId, minRating, maxRating);
        } else {
            reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        
        return reviews.stream()
            .map(this::enrichReviewResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public ReviewStatsResponse getUserStats(Long userId) {
        long totalReviews = reviewRepository.countByUserId(userId);
        
        Double avgRating = reviewRepository.getAverageRatingForUser(userId);
        double averageRating = avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0;
        
        List<Object[]> distribution = reviewRepository.getRatingDistributionForUser(userId);
        
        long fiveStar = 0, fourStar = 0, threeStar = 0, twoStar = 0, oneStar = 0;
        
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            
            if (rating == 5) fiveStar = count;
            else if (rating == 4) fourStar = count;
            else if (rating == 3) threeStar = count;
            else if (rating == 2) twoStar = count;
            else if (rating == 1) oneStar = count;
        }
        
        ReviewStatsResponse response = new ReviewStatsResponse();
        response.setTotalReviews(totalReviews);
        response.setAverageRating(averageRating);
        response.setFiveStarCount(fiveStar);
        response.setFourStarCount(fourStar);
        response.setThreeStarCount(threeStar);
        response.setTwoStarCount(twoStar);
        response.setOneStarCount(oneStar);
        
        return response;
    }
    
    @Override
    public AverageRatingResponse getContentAverageRating(Integer tmdbId) {
        Double avgRating = reviewRepository.getAverageRatingForContent(tmdbId);
        long reviewCount = reviewRepository.getReviewCountForContent(tmdbId);
        
        double averageRating = avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0;
        
        return new AverageRatingResponse(averageRating, reviewCount);
    }
    
    @Override
    public HasReviewedResponse checkUserReviewedContent(Long userId, Integer tmdbId) {
        boolean hasReviewed = reviewRepository.existsByUserIdAndTmdbId(userId, tmdbId);
        Integer rating = null;
        
        if (hasReviewed) {
            Review review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId).orElse(null);
            if (review != null) {
                rating = review.getRating();
            }
        }
        
        return new HasReviewedResponse(hasReviewed, rating);
    }
    
    private ReviewResponse enrichReviewResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setTmdbId(review.getTmdbId());
        response.setContentType(review.getContentType());
        response.setRating(review.getRating());
        response.setReviewText(review.getReviewText());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        
        String displayName = currentUserService.getUserDisplayName(review.getUserId());
        ReviewResponse.UserInfo userInfo = new ReviewResponse.UserInfo(review.getUserId(), displayName);
        response.setUser(userInfo);
        
        try {
            ContentResponse content = catalogClient.getContentById(review.getTmdbId(), review.getContentType());
            if (content != null) {
                ReviewResponse.ContentInfo contentInfo = new ReviewResponse.ContentInfo(
                    review.getTmdbId(),
                    content.getTitle() != null ? content.getTitle() : "Unknown",
                    content.getPosterUrl()
                );
                response.setContent(contentInfo);
            } else {
                ReviewResponse.ContentInfo contentInfo = new ReviewResponse.ContentInfo(
                    review.getTmdbId(),
                    "Unknown",
                    null
                );
                response.setContent(contentInfo);
            }
        } catch (Exception e) {
            ReviewResponse.ContentInfo contentInfo = new ReviewResponse.ContentInfo(
                review.getTmdbId(),
                "Unknown",
                null
            );
            response.setContent(contentInfo);
        }
        
        return response;
    }
}