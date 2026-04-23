// src/main/java/com/bingeboxed/watchlist/controller/WatchlistApiController.java
package com.bingeboxed.watchlist.controller;

import com.bingeboxed.watchlist.dto.AddToWatchlistRequest;
import com.bingeboxed.watchlist.dto.ContainsResponse;
import com.bingeboxed.watchlist.dto.UpdateStatusRequest;
import com.bingeboxed.watchlist.dto.WatchlistEntryResponse;
import com.bingeboxed.watchlist.dto.WatchlistStatsResponse;
import com.bingeboxed.watchlist.service.WatchlistService;
import com.bingeboxed.shared.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistApiController {

    private final WatchlistService watchlistService;
    private final JwtService       jwtService;
    private final UserResolver     userResolver;

    public WatchlistApiController(WatchlistService watchlistService,
                                  JwtService jwtService,
                                  UserResolver userResolver) {
        this.watchlistService = watchlistService;
        this.jwtService       = jwtService;
        this.userResolver     = userResolver;
    }

    // FR-01
    @PostMapping
    public ResponseEntity<WatchlistEntryResponse> add(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AddToWatchlistRequest request) {

        Long userId = resolveUserId(authHeader);
        WatchlistEntryResponse response = watchlistService.add(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // FR-02
    @DeleteMapping("/{tmdbId}")
    public ResponseEntity<Void> remove(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String tmdbId) {

        Long userId = resolveUserId(authHeader);
        watchlistService.remove(userId, parseTmdbId(tmdbId));
        return ResponseEntity.noContent().build();
    }

    // FR-03
    @PatchMapping("/{tmdbId}")
    public ResponseEntity<WatchlistEntryResponse> updateStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String tmdbId,
            @RequestBody UpdateStatusRequest request) {

        Long userId = resolveUserId(authHeader);
        WatchlistEntryResponse response =
                watchlistService.updateStatus(userId, parseTmdbId(tmdbId), request);
        return ResponseEntity.ok(response);
    }

    // FR-04
    @GetMapping
    public ResponseEntity<List<WatchlistEntryResponse>> getWatchlist(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        Long userId = resolveUserId(authHeader);
        return ResponseEntity.ok(watchlistService.getWatchlist(userId, status, type));
    }

    // FR-05 — must be declared before /{tmdbId} generic mappings to avoid ambiguity
    @GetMapping("/stats")
    public ResponseEntity<WatchlistStatsResponse> getStats(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = resolveUserId(authHeader);
        return ResponseEntity.ok(watchlistService.getStats(userId));
    }

    // FR-08
    @GetMapping("/contains/{tmdbId}")
    public ResponseEntity<ContainsResponse> contains(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String tmdbId) {

        Long userId = resolveUserId(authHeader);
        return ResponseEntity.ok(watchlistService.contains(userId, parseTmdbId(tmdbId)));
    }

    // FR-05
    @GetMapping("/{tmdbId}")
    public ResponseEntity<WatchlistEntryResponse> getEntry(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String tmdbId) {

        Long userId = resolveUserId(authHeader);
        return ResponseEntity.ok(watchlistService.getEntry(userId, parseTmdbId(tmdbId)));
    }

    // FR-06
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WatchlistEntryResponse>> getPublicWatchlist(
            @PathVariable Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be a positive integer");
        }
        return ResponseEntity.ok(watchlistService.getPublicWatchlist(userId, status, type));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Long resolveUserId(String authHeader) {
        String token = extractToken(authHeader);
        String email = jwtService.extractEmail(token);
        return userResolver.resolveUserIdByEmail(email);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authHeader.substring(7);
    }

    private Integer parseTmdbId(String raw) {
        try {
            int id = Integer.parseInt(raw);
            if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tmdbId must be a positive integer");
            return id;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tmdbId must be a valid integer");
        }
    }
}