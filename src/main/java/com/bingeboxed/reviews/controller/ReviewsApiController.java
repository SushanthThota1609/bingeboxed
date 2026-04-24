// src/main/java/com/bingeboxed/reviews/controller/ReviewsApiController.java
package com.bingeboxed.reviews.controller;

import com.bingeboxed.reviews.dto.*;
import com.bingeboxed.reviews.service.ReviewService;
import com.bingeboxed.shared.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewsApiController {

    private final ReviewService reviewService;
    private final JwtService    jwtService;

    public ReviewsApiController(ReviewService reviewService, JwtService jwtService) {
        this.reviewService = reviewService;
        this.jwtService    = jwtService;
    }

    // ── FR-01 ────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {

        String email = extractEmail(httpRequest);
        ReviewResponse response = reviewService.createReview(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── FR-02 ────────────────────────────────────────────────────────────────
    @PutMapping("/{tmdbId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Integer tmdbId,
            @RequestBody UpdateReviewRequest request,
            HttpServletRequest httpRequest) {

        String email = extractEmail(httpRequest);
        ReviewResponse response = reviewService.updateReview(email, tmdbId, request);
        return ResponseEntity.ok(response);
    }

    // ── FR-03 ────────────────────────────────────────────────────────────────
    @DeleteMapping("/{tmdbId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Integer tmdbId,
            HttpServletRequest httpRequest) {

        String email = extractEmail(httpRequest);
        reviewService.deleteReview(email, tmdbId);
        return ResponseEntity.noContent().build();
    }

    // ── FR-04 ────────────────────────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            HttpServletRequest httpRequest) {

        String email = extractEmail(httpRequest);
        return ResponseEntity.ok(reviewService.getMyReviews(email, type, minRating, maxRating));
    }

    // ── FR-05 ────────────────────────────────────────────────────────────────
    @GetMapping("/my/{tmdbId}")
    public ResponseEntity<ReviewResponse> getMyReviewForContent(
            @PathVariable Integer tmdbId,
            HttpServletRequest httpRequest) {

        String email = extractEmail(httpRequest);
        return ResponseEntity.ok(reviewService.getMyReviewForContent(email, tmdbId));
    }

    // ── FR-06 ────────────────────────────────────────────────────────────────
    @GetMapping("/content/{tmdbId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForContent(
            @PathVariable Integer tmdbId) {

        return ResponseEntity.ok(reviewService.getReviewsForContent(tmdbId));
    }

    // ── FR-07 ────────────────────────────────────────────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponse>> getPublicReviewsForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating) {

        return ResponseEntity.ok(
                reviewService.getPublicReviewsForUser(userId, type, minRating, maxRating));
    }

    // ── FR-08 ────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<ReviewStatsResponse> getMyStats(HttpServletRequest httpRequest) {
        String email = extractEmail(httpRequest);
        return ResponseEntity.ok(reviewService.getMyStats(email));
    }

    // ── FR-09 ────────────────────────────────────────────────────────────────
    @GetMapping("/content/{tmdbId}/rating")
    public ResponseEntity<ContentRatingResponse> getContentRating(
            @PathVariable Integer tmdbId) {

        return ResponseEntity.ok(reviewService.getContentRating(tmdbId));
    }

    // ── FR-10 ────────────────────────────────────────────────────────────────
    @GetMapping("/contains/{tmdbId}")
    public ResponseEntity<ContainsReviewResponse> containsReview(
            @PathVariable Integer tmdbId,
            HttpServletRequest httpRequest) {

        String email = extractEmail(httpRequest);
        return ResponseEntity.ok(reviewService.containsReview(email, tmdbId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return jwtService.extractEmail(token);
    }
}