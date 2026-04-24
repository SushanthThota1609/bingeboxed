// src/main/java/com/bingeboxed/reviews/controller/ReviewsApiController.java
package com.bingeboxed.reviews.controller;

import com.bingeboxed.reviews.dto.*;
import com.bingeboxed.reviews.service.ReviewService;
import com.bingeboxed.shared.security.JwtService;
import com.bingeboxed.shared.user.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewsApiController {
    
    private final ReviewService reviewService;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    
    public ReviewsApiController(ReviewService reviewService,
                                JwtService jwtService,
                                CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }
    
    private Long getCurrentUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        return currentUserService.getCurrentUserId(token);
    }
    
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@RequestHeader("Authorization") String authHeader,
                                                       @Valid @RequestBody CreateReviewRequest request) {
        Long userId = getCurrentUserId(authHeader);
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{tmdbId}")
    public ResponseEntity<ReviewResponse> updateReview(@RequestHeader("Authorization") String authHeader,
                                                       @PathVariable Integer tmdbId,
                                                       @Valid @RequestBody UpdateReviewRequest request) {
        Long userId = getCurrentUserId(authHeader);
        ReviewResponse response = reviewService.updateReview(userId, tmdbId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{tmdbId}")
    public ResponseEntity<Void> deleteReview(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable Integer tmdbId) {
        Long userId = getCurrentUserId(authHeader);
        reviewService.deleteReview(userId, tmdbId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(@RequestHeader("Authorization") String authHeader,
                                                             @RequestParam(required = false) String type,
                                                             @RequestParam(required = false) Integer minRating,
                                                             @RequestParam(required = false) Integer maxRating) {
        Long userId = getCurrentUserId(authHeader);
        
        if (minRating != null && (minRating < 1 || minRating > 5)) {
            throw new IllegalArgumentException("minRating must be between 1 and 5");
        }
        if (maxRating != null && (maxRating < 1 || maxRating > 5)) {
            throw new IllegalArgumentException("maxRating must be between 1 and 5");
        }
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new IllegalArgumentException("minRating cannot be greater than maxRating");
        }
        
        List<ReviewResponse> responses = reviewService.getUserReviews(userId, type, minRating, maxRating);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/my/{tmdbId}")
    public ResponseEntity<ReviewResponse> getMyReviewByTmdbId(@RequestHeader("Authorization") String authHeader,
                                                              @PathVariable Integer tmdbId) {
        Long userId = getCurrentUserId(authHeader);
        ReviewResponse response = reviewService.getUserReviewByTmdbId(userId, tmdbId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/content/{tmdbId}")
    public ResponseEntity<List<ReviewResponse>> getContentReviews(@PathVariable Integer tmdbId) {
        List<ReviewResponse> responses = reviewService.getContentReviews(tmdbId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(@PathVariable Long userId,
                                                               @RequestParam(required = false) String type,
                                                               @RequestParam(required = false) Integer minRating,
                                                               @RequestParam(required = false) Integer maxRating) {
        if (minRating != null && (minRating < 1 || minRating > 5)) {
            throw new IllegalArgumentException("minRating must be between 1 and 5");
        }
        if (maxRating != null && (maxRating < 1 || maxRating > 5)) {
            throw new IllegalArgumentException("maxRating must be between 1 and 5");
        }
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new IllegalArgumentException("minRating cannot be greater than maxRating");
        }
        
        List<ReviewResponse> responses = reviewService.getUserPublicReviews(userId, type, minRating, maxRating);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ReviewStatsResponse> getMyStats(@RequestHeader("Authorization") String authHeader) {
        Long userId = getCurrentUserId(authHeader);
        ReviewStatsResponse response = reviewService.getUserStats(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/content/{tmdbId}/rating")
    public ResponseEntity<AverageRatingResponse> getContentAverageRating(@PathVariable Integer tmdbId) {
        AverageRatingResponse response = reviewService.getContentAverageRating(tmdbId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/contains/{tmdbId}")
    public ResponseEntity<HasReviewedResponse> checkUserReviewedContent(@RequestHeader("Authorization") String authHeader,
                                                                        @PathVariable Integer tmdbId) {
        Long userId = getCurrentUserId(authHeader);
        HasReviewedResponse response = reviewService.checkUserReviewedContent(userId, tmdbId);
        return ResponseEntity.ok(response);
    }
}