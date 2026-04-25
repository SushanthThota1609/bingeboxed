package com.bingeboxed.recommendations.service;

import com.bingeboxed.recommendations.dto.ReasonResponse;
import com.bingeboxed.recommendations.dto.RecommendationResponse;

import java.util.List;

public interface RecommendationService {

    List<RecommendationResponse> generateRecommendations(Long userId);

    List<RecommendationResponse> getAllRecommendations(Long userId);

    RecommendationResponse getRecommendation(Long userId, Integer tmdbId);

    void deleteRecommendation(Long userId, Integer tmdbId);

    ReasonResponse getRecommendationReason(Long userId, Integer tmdbId);
}
