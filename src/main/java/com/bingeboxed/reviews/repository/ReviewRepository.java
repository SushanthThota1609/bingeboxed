// src/main/java/com/bingeboxed/reviews/repository/ReviewRepository.java
package com.bingeboxed.reviews.repository;

import com.bingeboxed.reviews.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Optional<Review> findByUserIdAndTmdbId(Long userId, Integer tmdbId);
    
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Review> findByUserIdAndContentTypeOrderByCreatedAtDesc(Long userId, String contentType);
    
    @Query("SELECT r FROM Review r WHERE r.userId = :userId " +
           "AND (:minRating IS NULL OR r.rating >= :minRating) " +
           "AND (:maxRating IS NULL OR r.rating <= :maxRating) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findUserReviewsWithRatingFilter(@Param("userId") Long userId,
                                                  @Param("minRating") Integer minRating,
                                                  @Param("maxRating") Integer maxRating);
    
    @Query("SELECT r FROM Review r WHERE r.userId = :userId " +
           "AND (:contentType IS NULL OR r.contentType = :contentType) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findUserReviewsWithTypeFilter(@Param("userId") Long userId,
                                               @Param("contentType") String contentType);
    
    List<Review> findByTmdbIdOrderByCreatedAtDesc(Integer tmdbId);
    
    @Query("SELECT r FROM Review r WHERE r.userId = :userId " +
           "AND (:contentType IS NULL OR r.contentType = :contentType) " +
           "AND (:minRating IS NULL OR r.rating >= :minRating) " +
           "AND (:maxRating IS NULL OR r.rating <= :maxRating) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findUserReviewsWithFilters(@Param("userId") Long userId,
                                            @Param("contentType") String contentType,
                                            @Param("minRating") Integer minRating,
                                            @Param("maxRating") Integer maxRating);
    
    List<Review> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tmdbId = :tmdbId")
    Double getAverageRatingForContent(@Param("tmdbId") Integer tmdbId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.tmdbId = :tmdbId")
    Long getReviewCountForContent(@Param("tmdbId") Integer tmdbId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.userId = :userId")
    Double getAverageRatingForUser(@Param("userId") Long userId);
    
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.userId = :userId GROUP BY r.rating")
    List<Object[]> getRatingDistributionForUser(@Param("userId") Long userId);
    
    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);
}