package com.bingeboxed.recommendations.repository;

import com.bingeboxed.recommendations.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    void deleteByUserId(Long userId);

    List<Recommendation> findByUserIdOrderByScoreDesc(Long userId);

    Optional<Recommendation> findByUserIdAndTmdbId(Long userId, Integer tmdbId);
}
