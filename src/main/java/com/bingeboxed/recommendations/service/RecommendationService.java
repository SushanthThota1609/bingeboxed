package com.bingeboxed.recommendations.service;

import com.bingeboxed.recommendations.dto.RecommendationResponse;

import java.util.List;

public interface RecommendationService {

    List<RecommendationResponse> generate(Long userId);

    List<RecommendationResponse> getForUser(Long userId);

    RecommendationResponse getByTmdbId(Long userId, Integer tmdbId);

    void dismiss(Long userId, Integer tmdbId);

    RecommendationResponse getReasonByTmdbId(Long userId, Integer tmdbId);
}
