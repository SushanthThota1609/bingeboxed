package com.bingeboxed.watchlist.service;

import com.bingeboxed.watchlist.dto.WatchlistResponse;
import com.bingeboxed.watchlist.dto.WatchlistStatsResponse;
import com.bingeboxed.watchlist.entity.WatchlistEntry;

import java.util.List;

public interface WatchlistService {

    WatchlistResponse addToWatchlist(Long userId, Integer tmdbId, String contentType);

    void removeFromWatchlist(Long userId, Integer tmdbId);

    WatchlistResponse updateStatus(Long userId, Integer tmdbId, String status);

    List<WatchlistResponse> getUserWatchlist(Long userId, String status, String contentType);

    List<WatchlistResponse> getPublicWatchlist(Long userId, String status, String contentType);

    WatchlistResponse getEntry(Long userId, Integer tmdbId);

    boolean existsByUserAndTmdbId(Long userId, Integer tmdbId);

    WatchlistStatsResponse getStats(Long userId);
}