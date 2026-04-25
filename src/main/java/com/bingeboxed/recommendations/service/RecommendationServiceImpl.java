package com.bingeboxed.recommendations.service;

import com.bingeboxed.catalog.entity.CatalogContent;
import com.bingeboxed.catalog.repository.CatalogContentRepository;
import com.bingeboxed.recommendations.dto.ReasonResponse;
import com.bingeboxed.recommendations.dto.RecommendationResponse;
import com.bingeboxed.recommendations.entity.Recommendation;
import com.bingeboxed.recommendations.repository.RecommendationRepository;
import com.bingeboxed.reviews.entity.Review;
import com.bingeboxed.reviews.repository.ReviewRepository;
import com.bingeboxed.shared.client.SocialGraphClient;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import com.bingeboxed.watchlist.entity.WatchlistEntry;
import com.bingeboxed.watchlist.repository.WatchlistRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final WatchlistRepository watchlistRepository;
    private final ReviewRepository reviewRepository;
    private final SocialGraphClient socialGraphClient;
    private final CatalogContentRepository catalogContentRepository;

    public RecommendationServiceImpl(RecommendationRepository recommendationRepository,
                                     WatchlistRepository watchlistRepository,
                                     ReviewRepository reviewRepository,
                                     SocialGraphClient socialGraphClient,
                                     CatalogContentRepository catalogContentRepository) {
        this.recommendationRepository = recommendationRepository;
        this.watchlistRepository = watchlistRepository;
        this.reviewRepository = reviewRepository;
        this.socialGraphClient = socialGraphClient;
        this.catalogContentRepository = catalogContentRepository;
    }

    @Override
    @Transactional
    public List<RecommendationResponse> generateRecommendations(Long userId) {
        recommendationRepository.deleteByUserId(userId);

        List<WatchlistEntry> watchlist = watchlistRepository.findByUserId(userId, Sort.unsorted());
        Set<Integer> excludedTmdbIds = watchlist.stream()
                .map(WatchlistEntry::getTmdbId)
                .collect(Collectors.toSet());

        List<Review> userReviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        excludedTmdbIds.addAll(userReviews.stream().map(Review::getTmdbId).collect(Collectors.toSet()));

        Map<Integer, Review> friendRecommendations = new HashMap<>();
        List<Long> friendIds = socialGraphClient.getFollowing(userId);
        for (Long friendId : friendIds) {
            List<Review> friendReviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(friendId);
            for (Review review : friendReviews) {
                if (review.getRating() >= 4 && !excludedTmdbIds.contains(review.getTmdbId())) {
                    friendRecommendations.merge(review.getTmdbId(), review, (existing, incoming) ->
                            existing.getRating() >= incoming.getRating() ? existing : incoming);
                }
            }
        }

        Map<String, List<CatalogContent>> genreToContent = new HashMap<>();
        for (Review review : userReviews) {
            if (review.getRating() >= 4) {
                Optional<CatalogContent> contentOpt = catalogContentRepository.findByTmdbId(review.getTmdbId());
                if (contentOpt.isPresent()) {
                    String genres = contentOpt.get().getGenre();
                    if (genres != null) {
                        for (String genre : genres.split(",\\s*")) {
                            genreToContent.computeIfAbsent(genre.trim(), k -> new ArrayList<>()).add(contentOpt.get());
                        }
                    }
                }
            }
        }

        Map<Integer, Recommendation> genreRecommendations = new HashMap<>();
        List<CatalogContent> allCatalog = catalogContentRepository.findAll();
        for (String genre : genreToContent.keySet()) {
            for (CatalogContent content : allCatalog) {
                if (excludedTmdbIds.contains(content.getTmdbId())) continue;
                String contentGenres = content.getGenre();
                if (contentGenres != null && Arrays.asList(contentGenres.split(",\\s*")).contains(genre)) {
                    genreRecommendations.merge(content.getTmdbId(),
                            new Recommendation(userId, content.getTmdbId(), content.getContentType(),
                                    new BigDecimal("70.00"), "Because you like " + genre),
                            (existing, incoming) -> {
                                if (incoming.getScore().compareTo(existing.getScore()) > 0) {
                                    return incoming;
                                }
                                return existing;
                            });
                }
            }
        }

        Map<Integer, Recommendation> allRecommendations = new HashMap<>();
        for (Map.Entry<Integer, Review> entry : friendRecommendations.entrySet()) {
            Review review = entry.getValue();
            BigDecimal score = review.getRating() == 5 ? new BigDecimal("95.00") : new BigDecimal("85.00");
            allRecommendations.put(entry.getKey(),
                    new Recommendation(userId, review.getTmdbId(), review.getContentType(),
                            score, "Friend rated " + review.getRating() + " stars"));
        }
        for (Map.Entry<Integer, Recommendation> entry : genreRecommendations.entrySet()) {
            allRecommendations.merge(entry.getKey(), entry.getValue(), (existing, incoming) -> {
                if (incoming.getScore().compareTo(existing.getScore()) > 0) {
                    return incoming;
                }
                return existing;
            });
        }

        List<Recommendation> saved = recommendationRepository.saveAll(
                allRecommendations.values().stream()
                        .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                        .limit(20)
                        .collect(Collectors.toList())
        );

        return saved.stream()
                .map(this::enrichRecommendation)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommendationResponse> getAllRecommendations(Long userId) {
        List<Recommendation> recommendations = recommendationRepository.findByUserIdOrderByScoreDesc(userId);
        return recommendations.stream().map(this::enrichRecommendation).collect(Collectors.toList());
    }

    @Override
    public RecommendationResponse getRecommendation(Long userId, Integer tmdbId) {
        Recommendation recommendation = recommendationRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found"));
        return enrichRecommendation(recommendation);
    }

    @Override
    public void deleteRecommendation(Long userId, Integer tmdbId) {
        Recommendation recommendation = recommendationRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found"));
        recommendationRepository.delete(recommendation);
    }

    @Override
    public ReasonResponse getRecommendationReason(Long userId, Integer tmdbId) {
        Recommendation recommendation = recommendationRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found"));
        return new ReasonResponse(recommendation.getReason(), recommendation.getScore());
    }

    private RecommendationResponse enrichRecommendation(Recommendation recommendation) {
        RecommendationResponse response = new RecommendationResponse();
        response.setId(recommendation.getId());
        response.setTmdbId(recommendation.getTmdbId());
        response.setContentType(recommendation.getContentType());
        response.setScore(recommendation.getScore());
        response.setReason(recommendation.getReason());
        response.setGeneratedAt(recommendation.getGeneratedAt());

        Optional<CatalogContent> contentOpt = catalogContentRepository.findByTmdbId(recommendation.getTmdbId());
        if (contentOpt.isPresent()) {
            CatalogContent content = contentOpt.get();
            response.setTitle(content.getTitle());
            response.setOverview(content.getOverview());
            response.setReleaseYear(content.getReleaseYear());
            response.setGenre(content.getGenre());
            response.setPosterUrl(content.getPosterUrl());
        } else {
            response.setTitle("Unknown");
            response.setOverview("");
            response.setReleaseYear(null);
            response.setGenre("");
            response.setPosterUrl("");
        }

        return response;
    }
}
