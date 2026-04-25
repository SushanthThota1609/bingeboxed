package com.bingeboxed.recommendations.controller;

import com.bingeboxed.recommendations.dto.ReasonResponse;
import com.bingeboxed.recommendations.dto.RecommendationResponse;
import com.bingeboxed.recommendations.service.RecommendationService;
import com.bingeboxed.shared.security.JwtService;
import com.bingeboxed.shared.user.UserResolverService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final JwtService jwtService;
    private final UserResolverService userResolverService;

    public RecommendationApiController(
            RecommendationService recommendationService,
            JwtService jwtService,
            UserResolverService userResolverService) {
        this.recommendationService = recommendationService;
        this.jwtService = jwtService;
        this.userResolverService = userResolverService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<RecommendationResponse>> generate(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = resolveUserId(authHeader);
        List<RecommendationResponse> result = recommendationService.generate(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getMyRecommendations(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = resolveUserId(authHeader);
        return ResponseEntity.ok(recommendationService.getForUser(userId));
    }

    @GetMapping("/{tmdbId}")
    public ResponseEntity<RecommendationResponse> getOne(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tmdbId) {
        Long userId = resolveUserId(authHeader);
        return ResponseEntity.ok(recommendationService.getByTmdbId(userId, tmdbId));
    }

    @DeleteMapping("/{tmdbId}")
    public ResponseEntity<Void> dismiss(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tmdbId) {
        Long userId = resolveUserId(authHeader);
        recommendationService.dismiss(userId, tmdbId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tmdbId}/reason")
    public ResponseEntity<ReasonResponse> getReason(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer tmdbId) {
        Long userId = resolveUserId(authHeader);
        RecommendationResponse rec = recommendationService.getReasonByTmdbId(userId, tmdbId);
        return ResponseEntity.ok(new ReasonResponse(rec.getReason(), rec.getScore()));
    }

    private Long resolveUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        String email = jwtService.extractEmail(token);
        return userResolverService.resolveUserId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
