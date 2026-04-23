package com.bingeboxed.watchlist.controller;

import com.bingeboxed.auth.entity.User;
import com.bingeboxed.auth.repository.UserRepository;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import com.bingeboxed.shared.security.JwtService;
import com.bingeboxed.watchlist.dto.ContainsResponse;
import com.bingeboxed.watchlist.dto.WatchlistRequest;
import com.bingeboxed.watchlist.dto.WatchlistResponse;
import com.bingeboxed.watchlist.dto.WatchlistStatsResponse;
import com.bingeboxed.watchlist.dto.WatchlistUpdateRequest;
import com.bingeboxed.watchlist.service.WatchlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistApiController {

    private final WatchlistService watchlistService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WatchlistApiController(WatchlistService watchlistService, JwtService jwtService, UserRepository userRepository) {
        this.watchlistService = watchlistService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid token");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        if (email == null || !jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> addToWatchlist(@Valid @RequestBody WatchlistRequest request,
                                                            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        try {
            WatchlistResponse response = watchlistService.addToWatchlist(userId, request.getTmdbId(), request.getContentType());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Entry already exists for this user");
        }
    }

    @DeleteMapping("/{tmdbId}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Integer tmdbId,
                                                    HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        watchlistService.removeFromWatchlist(userId, tmdbId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{tmdbId}")
    public ResponseEntity<WatchlistResponse> updateStatus(@PathVariable Integer tmdbId,
                                                          @Valid @RequestBody WatchlistUpdateRequest request,
                                                          HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        WatchlistResponse response = watchlistService.updateStatus(userId, tmdbId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WatchlistResponse>> getMyWatchlist(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        List<WatchlistResponse> entries = watchlistService.getUserWatchlist(userId, status, type);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{tmdbId}")
    public ResponseEntity<WatchlistResponse> getEntry(@PathVariable Integer tmdbId,
                                                      HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        WatchlistResponse response = watchlistService.getEntry(userId, tmdbId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WatchlistResponse>> getPublicWatchlist(@PathVariable Long userId,
                                                                      @RequestParam(required = false) String status,
                                                                      @RequestParam(required = false) String type) {
        List<WatchlistResponse> entries = watchlistService.getPublicWatchlist(userId, status, type);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/stats")
    public ResponseEntity<WatchlistStatsResponse> getStats(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        WatchlistStatsResponse stats = watchlistService.getStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/contains/{tmdbId}")
    public ResponseEntity<ContainsResponse> contains(@PathVariable Integer tmdbId,
                                                     HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        boolean exists = watchlistService.existsByUserAndTmdbId(userId, tmdbId);
        String status = null;
        if (exists) {
            // We need the status; could fetch entry but that's extra query. For simplicity, fetch entry.
            WatchlistResponse entry = watchlistService.getEntry(userId, tmdbId);
            status = entry.getStatus();
        }
        return ResponseEntity.ok(new ContainsResponse(exists, status));
    }
}