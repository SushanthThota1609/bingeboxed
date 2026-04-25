package com.bingeboxed.recommendations.controller;

import com.bingeboxed.recommendations.dto.ReasonResponse;
import com.bingeboxed.recommendations.dto.RecommendationResponse;
import com.bingeboxed.recommendations.service.RecommendationService;
import com.bingeboxed.shared.user.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final CurrentUserService currentUserService;

    public RecommendationApiController(RecommendationService recommendationService,
                                       CurrentUserService currentUserService) {
        this.recommendationService = recommendationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<RecommendationResponse>> generate(HttpServletRequest request) {
        Long userId = extractUserId(request);
        List<RecommendationResponse> generated = recommendationService.generateRecommendations(userId);
        return ResponseEntity.ok(generated);
    }

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getAll(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(recommendationService.getAllRecommendations(userId));
    }

    @GetMapping("/{tmdbId}")
    public ResponseEntity<RecommendationResponse> getOne(@PathVariable Integer tmdbId,
                                                         HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(recommendationService.getRecommendation(userId, tmdbId));
    }

    @DeleteMapping("/{tmdbId}")
    public ResponseEntity<Void> delete(@PathVariable Integer tmdbId, HttpServletRequest request) {
        Long userId = extractUserId(request);
        recommendationService.deleteRecommendation(userId, tmdbId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tmdbId}/reason")
    public ResponseEntity<ReasonResponse> getReason(@PathVariable Integer tmdbId,
                                                    HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(recommendationService.getRecommendationReason(userId, tmdbId));
    }

    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid or missing token");
        }
        String token = header.substring(7);
        return currentUserService.getCurrentUserId(token);
    }
}
