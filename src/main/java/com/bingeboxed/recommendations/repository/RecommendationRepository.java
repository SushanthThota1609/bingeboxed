package com.bingeboxed.recommendations.repository;

import com.bingeboxed.recommendations.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByScoreDesc(Long userId);

    Optional<Recommendation> findByUserIdAndTmdbId(Long userId, Integer tmdbId);

    @Modifying
    @Query("DELETE FROM Recommendation r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);
}
