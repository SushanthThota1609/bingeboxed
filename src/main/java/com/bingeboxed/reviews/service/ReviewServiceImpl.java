// src/main/java/com/bingeboxed/reviews/service/ReviewServiceImpl.java
package com.bingeboxed.reviews.service;

import com.bingeboxed.reviews.dto.*;
import com.bingeboxed.reviews.entity.Review;
import com.bingeboxed.reviews.repository.ReviewRepository;
import com.bingeboxed.shared.client.CatalogClient;
import com.bingeboxed.shared.client.CatalogContentDto;
import com.bingeboxed.shared.client.CatalogUnavailableException;
import com.bingeboxed.shared.user.UserResolverService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Set<String> VALID_TYPES = Set.of("MOVIE", "SERIES");

    private final ReviewRepository    reviewRepository;
    private final CatalogClient       catalogClient;
    private final UserResolverService userResolverService;
    private final JdbcTemplate        jdbcTemplate;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             CatalogClient catalogClient,
                             UserResolverService userResolverService,
                             JdbcTemplate jdbcTemplate) {
        this.reviewRepository    = reviewRepository;
        this.catalogClient       = catalogClient;
        this.userResolverService = userResolverService;
        this.jdbcTemplate        = jdbcTemplate;
    }

    // ── FR-01 ────────────────────────────────────────────────────────────────
    @Override
    public ReviewResponse createReview(String email, CreateReviewRequest req) {
        validateTmdbId(req.getTmdbId());
        validateContentType(req.getContentType());
        validateRating(req.getRating());

        Long userId = resolveUserId(email);

        if (reviewRepository.existsByUserIdAndTmdbId(userId, req.getTmdbId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already reviewed this content");
        }

        CatalogContentDto content = fetchContent(req.getTmdbId(), req.getContentType());

        Review review = new Review();
        review.setUserId(userId);
        review.setTmdbId(req.getTmdbId());
        review.setContentType(req.getContentType().toUpperCase());
        review.setRating(req.getRating());
        review.setReviewText(req.getReviewText());

        try {
            review = reviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already reviewed this content");
        }

        return toResponse(review, content, resolveDisplayName(userId));
    }

    // ── FR-02 ────────────────────────────────────────────────────────────────
    @Override
    public ReviewResponse updateReview(String email, Integer tmdbId, UpdateReviewRequest req) {
        validateTmdbId(tmdbId);
        validateRating(req.getRating());

        Long userId = resolveUserId(email);

        Review review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Review not found"));

        if (!review.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You may not modify another user's review");
        }

        review.setRating(req.getRating());
        review.setReviewText(req.getReviewText());
        review = reviewRepository.save(review);

        CatalogContentDto content = fetchContentSilent(tmdbId, review.getContentType());
        return toResponse(review, content, resolveDisplayName(userId));
    }

    // ── FR-03 ────────────────────────────────────────────────────────────────
    @Override
    public void deleteReview(String email, Integer tmdbId) {
        validateTmdbId(tmdbId);

        Long userId = resolveUserId(email);

        Review review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Review not found"));

        if (!review.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You may not delete another user's review");
        }

        reviewRepository.delete(review);
    }

    // ── FR-04 ────────────────────────────────────────────────────────────────
    @Override
    public List<ReviewResponse> getMyReviews(String email, String type,
                                              Integer minRating, Integer maxRating) {
        validateOptionalType(type);
        validateOptionalRatingRange(minRating, maxRating);

        Long userId = resolveUserId(email);
        String normalizedType = (type != null && !type.isBlank()) ? type.toUpperCase() : null;

        List<Review> reviews = reviewRepository.findByUserIdFiltered(
                userId, normalizedType, minRating, maxRating);

        String displayName = resolveDisplayName(userId);
        return reviews.stream()
                .map(r -> toResponse(r, fetchContentSilent(r.getTmdbId(), r.getContentType()), displayName))
                .collect(Collectors.toList());
    }

    // ── FR-05 ────────────────────────────────────────────────────────────────
    @Override
    public ReviewResponse getMyReviewForContent(String email, Integer tmdbId) {
        validateTmdbId(tmdbId);

        Long userId = resolveUserId(email);

        Review review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Review not found"));

        CatalogContentDto content = fetchContentSilent(tmdbId, review.getContentType());
        return toResponse(review, content, resolveDisplayName(userId));
    }

    // ── FR-06 ────────────────────────────────────────────────────────────────
    @Override
    public List<ReviewResponse> getReviewsForContent(Integer tmdbId) {
        validateTmdbId(tmdbId);

        List<Review> reviews = reviewRepository.findByTmdbIdOrderByCreatedAtDesc(tmdbId);

        return reviews.stream()
                .map(r -> toResponse(r, fetchContentSilent(tmdbId, r.getContentType()),
                        resolveDisplayName(r.getUserId())))
                .collect(Collectors.toList());
    }

    // ── FR-07 ────────────────────────────────────────────────────────────────
    @Override
    public List<ReviewResponse> getPublicReviewsForUser(Long userId, String type,
                                                         Integer minRating, Integer maxRating) {
        validateOptionalType(type);
        validateOptionalRatingRange(minRating, maxRating);

        String normalizedType = (type != null && !type.isBlank()) ? type.toUpperCase() : null;

        List<Review> reviews = reviewRepository.findByUserIdPublicFiltered(
                userId, normalizedType, minRating, maxRating);

        String displayName = resolveDisplayName(userId);
        return reviews.stream()
                .map(r -> toResponse(r, fetchContentSilent(r.getTmdbId(), r.getContentType()), displayName))
                .collect(Collectors.toList());
    }

    // ── FR-08 ────────────────────────────────────────────────────────────────
    @Override
    public ReviewStatsResponse getMyStats(String email) {
        Long userId = resolveUserId(email);

        long total   = reviewRepository.countByUserId(userId);
        double avg   = reviewRepository.averageRatingByUserId(userId);
        double rounded = round1dp(avg);

        ReviewStatsResponse stats = new ReviewStatsResponse();
        stats.setTotalReviews(total);
        stats.setAverageRating(rounded);
        stats.setFiveStarCount( reviewRepository.countByUserIdAndRating(userId, 5));
        stats.setFourStarCount( reviewRepository.countByUserIdAndRating(userId, 4));
        stats.setThreeStarCount(reviewRepository.countByUserIdAndRating(userId, 3));
        stats.setTwoStarCount(  reviewRepository.countByUserIdAndRating(userId, 2));
        stats.setOneStarCount(  reviewRepository.countByUserIdAndRating(userId, 1));
        return stats;
    }

    // ── FR-09 ────────────────────────────────────────────────────────────────
    @Override
    public ContentRatingResponse getContentRating(Integer tmdbId) {
        validateTmdbId(tmdbId);

        double avg   = reviewRepository.averageRatingByTmdbId(tmdbId);
        long   count = reviewRepository.countByTmdbId(tmdbId);
        return new ContentRatingResponse(round1dp(avg), count);
    }

    // ── FR-10 ────────────────────────────────────────────────────────────────
    @Override
    public ContainsReviewResponse containsReview(String email, Integer tmdbId) {
        validateTmdbId(tmdbId);

        Long userId = resolveUserId(email);

        Optional<Review> review = reviewRepository.findByUserIdAndTmdbId(userId, tmdbId);
        if (review.isEmpty()) {
            return new ContainsReviewResponse(false, null);
        }
        return new ContainsReviewResponse(true, review.get().getRating());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveUserId(String email) {
        return userResolverService.resolveUserId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found"));
    }

    private String resolveDisplayName(Long userId) {
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT display_name FROM profiles WHERE user_id = ?", String.class, userId);
            if (!names.isEmpty() && names.get(0) != null && !names.get(0).isBlank()) {
                return names.get(0);
            }
        } catch (Exception ignored) {}
        return "User " + userId;
    }

    private CatalogContentDto fetchContent(Integer tmdbId, String contentType) {
        try {
            return catalogClient.findById(tmdbId, contentType)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Content not found in catalog"));
        } catch (CatalogUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Catalog service is unavailable");
        }
    }

    private CatalogContentDto fetchContentSilent(Integer tmdbId, String contentType) {
        try {
            return catalogClient.findById(tmdbId, contentType).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ReviewResponse toResponse(Review review, CatalogContentDto content, String displayName) {
        ReviewResponse resp = new ReviewResponse();
        resp.setId(review.getId());
        resp.setUserId(review.getUserId());
        resp.setDisplayName(displayName);
        resp.setTmdbId(review.getTmdbId());
        resp.setContentType(review.getContentType());
        resp.setRating(review.getRating());
        resp.setReviewText(review.getReviewText());
        resp.setCreatedAt(review.getCreatedAt());
        resp.setUpdatedAt(review.getUpdatedAt());

        if (content != null) {
            resp.setContentTitle(content.getTitle());
            resp.setContentPosterUrl(content.getPosterUrl());
            resp.setContentReleaseYear(content.getReleaseYear());
            resp.setContentGenre(content.getGenre());
        }
        return resp;
    }

    private void validateTmdbId(Integer tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tmdbId");
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || !VALID_TYPES.contains(contentType.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "contentType must be MOVIE or SERIES");
        }
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "rating must be between 1 and 5");
        }
    }

    private void validateOptionalType(String type) {
        if (type != null && !type.isBlank() && !VALID_TYPES.contains(type.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "type filter must be MOVIE or SERIES");
        }
    }

    private void validateOptionalRatingRange(Integer min, Integer max) {
        if (min != null && (min < 1 || min > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "minRating must be between 1 and 5");
        }
        if (max != null && (max < 1 || max > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "maxRating must be between 1 and 5");
        }
        if (min != null && max != null && min > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "minRating must not exceed maxRating");
        }
    }

    private double round1dp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}