package com.bingeboxed.recommendations.service;

import com.bingeboxed.catalog.entity.CatalogContent;
import com.bingeboxed.catalog.repository.CatalogContentRepository;
import com.bingeboxed.recommendations.dto.RecommendationResponse;
import com.bingeboxed.recommendations.entity.Recommendation;
import com.bingeboxed.recommendations.repository.RecommendationRepository;
import com.bingeboxed.reviews.entity.Review;
import com.bingeboxed.reviews.repository.ReviewRepository;
import com.bingeboxed.shared.client.CatalogClient;
import com.bingeboxed.shared.client.CatalogContentDto;
import com.bingeboxed.social.repository.FriendshipRepository;
import com.bingeboxed.watchlist.repository.WatchlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final int HIGH_RATING_THRESHOLD = 6;
    private static final int MAX_RECOMMENDATIONS   = 20;

    private final RecommendationRepository  recommendationRepository;
    private final WatchlistRepository       watchlistRepository;
    private final ReviewRepository          reviewRepository;
    private final FriendshipRepository      friendshipRepository;
    private final CatalogContentRepository  catalogContentRepository;
    private final CatalogClient             catalogClient;
    private final EntityManager             entityManager;

    public RecommendationServiceImpl(
            RecommendationRepository recommendationRepository,
            WatchlistRepository watchlistRepository,
            ReviewRepository reviewRepository,
            FriendshipRepository friendshipRepository,
            CatalogContentRepository catalogContentRepository,
            CatalogClient catalogClient,
            EntityManager entityManager) {
        this.recommendationRepository = recommendationRepository;
        this.watchlistRepository      = watchlistRepository;
        this.reviewRepository         = reviewRepository;
        this.friendshipRepository     = friendshipRepository;
        this.catalogContentRepository = catalogContentRepository;
        this.catalogClient            = catalogClient;
        this.entityManager            = entityManager;
    }

    @Override
    @Transactional
    public List<RecommendationResponse> generate(Long userId) {
        recommendationRepository.deleteByUserId(userId);
        entityManager.flush();

        Set<Integer>             excluded  = buildExclusionSet(userId);
        Map<Integer, BigDecimal> scoreMap  = new HashMap<>();
        Map<Integer, String>     reasonMap = new HashMap<>();
        Map<Integer, String>     typeMap   = new HashMap<>();

        scoreFromWatchlistGenres(userId, excluded, scoreMap, reasonMap, typeMap);
        scoreFromFriendRatings(userId, excluded, scoreMap, reasonMap, typeMap);
        scoreFromUserHighRatings(userId, excluded, scoreMap, reasonMap, typeMap);

        if (scoreMap.isEmpty()) {
            scoreFallback(excluded, scoreMap, reasonMap, typeMap);
        }

        List<Recommendation> toSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                .limit(MAX_RECOMMENDATIONS)
                .forEach(entry -> {
                    Integer tmdbId = entry.getKey();
                    Recommendation rec = new Recommendation();
                    rec.setUserId(userId);
                    rec.setTmdbId(tmdbId);
                    rec.setContentType(typeMap.getOrDefault(tmdbId, "MOVIE"));
                    rec.setScore(entry.getValue().min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
                    rec.setReason(reasonMap.getOrDefault(tmdbId, "Recommended for you"));
                    rec.setGeneratedAt(now);
                    toSave.add(rec);
                });

        List<Recommendation> saved = recommendationRepository.saveAll(toSave);
        return saved.stream().map(this::enrich).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getForUser(Long userId) {
        return recommendationRepository.findByUserIdOrderByScoreDesc(userId)
                .stream().map(this::enrich).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getByTmdbId(Long userId, Integer tmdbId) {
        Recommendation rec = recommendationRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation not found"));
        return enrich(rec);
    }

    @Override
    @Transactional
    public void dismiss(Long userId, Integer tmdbId) {
        Recommendation rec = recommendationRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation not found"));
        recommendationRepository.delete(rec);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getReasonByTmdbId(Long userId, Integer tmdbId) {
        return getByTmdbId(userId, tmdbId);
    }

    private Set<Integer> buildExclusionSet(Long userId) {
        Set<Integer> excluded = new HashSet<>();
        watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(e -> excluded.add(e.getTmdbId()));
        reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(r -> excluded.add(r.getTmdbId()));
        return excluded;
    }

    private void scoreFromWatchlistGenres(
            Long userId, Set<Integer> excluded,
            Map<Integer, BigDecimal> scoreMap,
            Map<Integer, String> reasonMap,
            Map<Integer, String> typeMap) {

        watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(e -> catalogContentRepository.findByTmdbId(e.getTmdbId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(cc -> cc.getGenre() != null)
                .forEach(cc -> {
                    Map<String, Long> genreFreq = new HashMap<>();
                    for (String g : cc.getGenre().split(",")) {
                        String genre = g.trim().toUpperCase();
                        if (!genre.isEmpty()) genreFreq.merge(genre, 1L, Long::sum);
                    }
                    genreFreq.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .ifPresent(topGenre -> {
                                for (CatalogContent candidate : catalogContentRepository.findAll()) {
                                    if (excluded.contains(candidate.getTmdbId()) || candidate.getGenre() == null) continue;
                                    for (String g : candidate.getGenre().split(",")) {
                                        if (g.trim().equalsIgnoreCase(topGenre)) {
                                            scoreMap.merge(candidate.getTmdbId(), BigDecimal.valueOf(60), BigDecimal::add);
                                            reasonMap.putIfAbsent(candidate.getTmdbId(), "Matches your favorite genre: " + topGenre);
                                            typeMap.putIfAbsent(candidate.getTmdbId(), candidate.getContentType());
                                            break;
                                        }
                                    }
                                }
                            });
                });
    }

    private void scoreFromFriendRatings(
            Long userId, Set<Integer> excluded,
            Map<Integer, BigDecimal> scoreMap,
            Map<Integer, String> reasonMap,
            Map<Integer, String> typeMap) {

        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(userId);
        if (friendIds.isEmpty()) return;

        for (Long friendId : friendIds) {
            for (Review r : reviewRepository.findByUserIdFiltered(friendId, null, HIGH_RATING_THRESHOLD, null)) {
                if (excluded.contains(r.getTmdbId())) continue;
                scoreMap.merge(r.getTmdbId(), BigDecimal.valueOf(r.getRating() * 3L), BigDecimal::add);
                reasonMap.putIfAbsent(r.getTmdbId(), "Highly rated by your friends");
                typeMap.putIfAbsent(r.getTmdbId(), r.getContentType());
            }
        }
    }

    private void scoreFromUserHighRatings(
            Long userId, Set<Integer> excluded,
            Map<Integer, BigDecimal> scoreMap,
            Map<Integer, String> reasonMap,
            Map<Integer, String> typeMap) {

        List<Review> highRated = reviewRepository.findByUserIdFiltered(userId, null, HIGH_RATING_THRESHOLD, null);
        if (highRated.isEmpty()) return;

        Map<String, Long> genreFreq = new HashMap<>();
        for (Review r : highRated) {
            catalogContentRepository.findByTmdbId(r.getTmdbId()).ifPresent(cc -> {
                if (cc.getGenre() != null) {
                    for (String g : cc.getGenre().split(",")) {
                        String genre = g.trim().toUpperCase();
                        if (!genre.isEmpty()) genreFreq.merge(genre, 1L, Long::sum);
                    }
                }
            });
        }

        if (genreFreq.isEmpty()) return;

        String topGenre = genreFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (topGenre == null) return;

        for (CatalogContent cc : catalogContentRepository.findAll()) {
            if (excluded.contains(cc.getTmdbId()) || cc.getGenre() == null) continue;
            for (String g : cc.getGenre().split(",")) {
                if (g.trim().equalsIgnoreCase(topGenre)) {
                    scoreMap.merge(cc.getTmdbId(), BigDecimal.valueOf(40), BigDecimal::add);
                    reasonMap.putIfAbsent(cc.getTmdbId(), "Popular in genres you love: " + topGenre);
                    typeMap.putIfAbsent(cc.getTmdbId(), cc.getContentType());
                    break;
                }
            }
        }
    }

    private void scoreFallback(
            Set<Integer> excluded,
            Map<Integer, BigDecimal> scoreMap,
            Map<Integer, String> reasonMap,
            Map<Integer, String> typeMap) {

        for (CatalogContent cc : catalogContentRepository.findAll()) {
            if (excluded.contains(cc.getTmdbId())) continue;
            int year = cc.getReleaseYear() != null ? cc.getReleaseYear() : 1970;
            BigDecimal score = BigDecimal.valueOf(Math.max(1, year - 1970));
            scoreMap.put(cc.getTmdbId(), score);
            reasonMap.put(cc.getTmdbId(), "Trending in the catalog");
            typeMap.put(cc.getTmdbId(), cc.getContentType());
        }
    }

    private RecommendationResponse enrich(Recommendation rec) {
        RecommendationResponse r = new RecommendationResponse();
        r.setId(rec.getId());
        r.setTmdbId(rec.getTmdbId());
        r.setContentType(rec.getContentType());
        r.setScore(rec.getScore());
        r.setReason(rec.getReason());
        r.setGeneratedAt(rec.getGeneratedAt());

        try {
            Optional<CatalogContentDto> dto = catalogClient.findById(rec.getTmdbId(), rec.getContentType());
            dto.ifPresent(d -> {
                r.setTitle(d.getTitle());
                r.setPosterUrl(d.getPosterUrl());
                r.setGenre(d.getGenre());
                r.setReleaseYear(d.getReleaseYear());
            });
        } catch (Exception ignored) {}

        if (r.getTitle() == null) {
            catalogContentRepository.findByTmdbId(rec.getTmdbId()).ifPresent(cc -> {
                r.setTitle(cc.getTitle());
                r.setPosterUrl(cc.getPosterUrl());
                r.setGenre(cc.getGenre());
                r.setReleaseYear(cc.getReleaseYear());
            });
        }

        return r;
    }
}
