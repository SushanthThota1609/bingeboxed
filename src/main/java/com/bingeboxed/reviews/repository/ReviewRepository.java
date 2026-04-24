// src/main/java/com/bingeboxed/reviews/repository/ReviewRepository.java
package com.bingeboxed.reviews.repository;

import com.bingeboxed.reviews.entity.Review;
import com.bingeboxed.reviews.dto.RatingDistributionRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndTmdbId(Long userId, Integer tmdbId);

    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM Review r WHERE r.userId = :userId " +
           "AND (:type IS NULL OR r.contentType = :type) " +
           "AND (:minRating IS NULL OR r.rating >= :minRating) " +
           "AND (:maxRating IS NULL OR r.rating <= :maxRating) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByUserIdFiltered(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating);

    @Query("SELECT r FROM Review r WHERE r.tmdbId = :tmdbId ORDER BY r.createdAt DESC")
    List<Review> findByTmdbIdOrderByCreatedAtDesc(@Param("tmdbId") Integer tmdbId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(AVG(CAST(r.rating AS double)), 0.0) FROM Review r WHERE r.userId = :userId")
    double averageRatingByUserId(@Param("userId") Long userId);

    @Query("SELECT r.rating FROM Review r WHERE r.userId = :userId GROUP BY r.rating")
    List<Integer> findDistinctRatingsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.userId = :userId AND r.rating = :rating")
    long countByUserIdAndRating(@Param("userId") Long userId, @Param("rating") int rating);

    @Query("SELECT COALESCE(AVG(CAST(r.rating AS double)), 0.0) FROM Review r WHERE r.tmdbId = :tmdbId")
    double averageRatingByTmdbId(@Param("tmdbId") Integer tmdbId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.tmdbId = :tmdbId")
    long countByTmdbId(@Param("tmdbId") Integer tmdbId);

    @Query("SELECT r FROM Review r WHERE r.userId = :userId " +
           "AND (:type IS NULL OR r.contentType = :type) " +
           "AND (:minRating IS NULL OR r.rating >= :minRating) " +
           "AND (:maxRating IS NULL OR r.rating <= :maxRating) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByUserIdPublicFiltered(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating);
}