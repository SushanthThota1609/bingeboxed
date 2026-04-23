// src/main/java/com/bingeboxed/watchlist/service/WatchlistService.java
package com.bingeboxed.watchlist.service;

import com.bingeboxed.watchlist.dto.AddToWatchlistRequest;
import com.bingeboxed.watchlist.dto.ContainsResponse;
import com.bingeboxed.watchlist.dto.UpdateStatusRequest;
import com.bingeboxed.watchlist.dto.WatchlistEntryResponse;
import com.bingeboxed.watchlist.dto.WatchlistStatsResponse;

import java.util.List;

public interface WatchlistService {

    WatchlistEntryResponse  add(Long userId, AddToWatchlistRequest request);
    void                    remove(Long userId, Integer tmdbId);
    WatchlistEntryResponse  updateStatus(Long userId, Integer tmdbId, UpdateStatusRequest request);
    List<WatchlistEntryResponse> getWatchlist(Long userId, String status, String type);
    WatchlistEntryResponse  getEntry(Long userId, Integer tmdbId);
    List<WatchlistEntryResponse> getPublicWatchlist(Long userId, String status, String type);
    WatchlistStatsResponse  getStats(Long userId);
    ContainsResponse        contains(Long userId, Integer tmdbId);
}