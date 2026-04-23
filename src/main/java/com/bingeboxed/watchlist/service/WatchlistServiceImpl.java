package com.bingeboxed.watchlist.service;

import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.shared.client.CatalogClient;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import com.bingeboxed.watchlist.dto.ContainsResponse;
import com.bingeboxed.watchlist.dto.WatchlistResponse;
import com.bingeboxed.watchlist.dto.WatchlistStatsResponse;
import com.bingeboxed.watchlist.entity.WatchlistEntry;
import com.bingeboxed.watchlist.entity.WatchlistEntry.ContentType;
import com.bingeboxed.watchlist.entity.WatchlistEntry.WatchlistStatus;
import com.bingeboxed.watchlist.repository.WatchlistRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final CatalogClient catalogClient;

    public WatchlistServiceImpl(WatchlistRepository watchlistRepository, CatalogClient catalogClient) {
        this.watchlistRepository = watchlistRepository;
        this.catalogClient = catalogClient;
    }

    @Override
    public WatchlistResponse addToWatchlist(Long userId, Integer tmdbId, String contentTypeStr) {
        // Validate contentType
        ContentType contentType;
        try {
            contentType = ContentType.valueOf(contentTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid contentType. Allowed: MOVIE, SERIES");
        }

        // Verify content exists via CatalogClient
        ContentResponse content = catalogClient.getContentById(tmdbId, contentTypeStr.toLowerCase());
        if (content == null) {
            throw new ResourceNotFoundException("Content not found in catalog");
        }

        // Check if already exists
        if (watchlistRepository.existsByUserIdAndTmdbId(userId, tmdbId)) {
            throw new DataIntegrityViolationException("Entry already exists for this user");
        }

        WatchlistEntry entry = new WatchlistEntry(userId, tmdbId, contentType, WatchlistStatus.WANT_TO_WATCH);
        WatchlistEntry saved = watchlistRepository.save(entry);
        return toResponse(saved, content);
    }

    @Override
    public void removeFromWatchlist(Long userId, Integer tmdbId) {
        WatchlistEntry entry = watchlistRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist entry not found"));
        watchlistRepository.delete(entry);
    }

    @Override
    public WatchlistResponse updateStatus(Long userId, Integer tmdbId, String statusStr) {
        WatchlistStatus status;
        try {
            status = WatchlistStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status. Allowed: WANT_TO_WATCH, WATCHING, COMPLETED");
        }

        WatchlistEntry entry = watchlistRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist entry not found"));
        entry.setStatus(status);
        WatchlistEntry updated = watchlistRepository.save(entry);

        ContentResponse content = catalogClient.getContentById(tmdbId, entry.getContentType().name().toLowerCase());
        return toResponse(updated, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistResponse> getUserWatchlist(Long userId, String status, String contentType) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<WatchlistEntry> entries;

        if (status != null && contentType != null) {
            WatchlistStatus watchlistStatus = WatchlistStatus.valueOf(status.toUpperCase());
            ContentType ct = ContentType.valueOf(contentType.toUpperCase());
            entries = watchlistRepository.findByUserIdAndStatusAndContentType(userId, watchlistStatus, ct, sort);
        } else if (status != null) {
            WatchlistStatus watchlistStatus = WatchlistStatus.valueOf(status.toUpperCase());
            entries = watchlistRepository.findByUserIdAndStatus(userId, watchlistStatus, sort);
        } else if (contentType != null) {
            ContentType ct = ContentType.valueOf(contentType.toUpperCase());
            entries = watchlistRepository.findByUserIdAndContentType(userId, ct, sort);
        } else {
            entries = watchlistRepository.findByUserId(userId, sort);
        }

        return entries.stream()
                .map(entry -> {
                    ContentResponse content = catalogClient.getContentById(entry.getTmdbId(), entry.getContentType().name().toLowerCase());
                    return toResponse(entry, content);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistResponse> getPublicWatchlist(Long userId, String status, String contentType) {
        // Same as getUserWatchlist but without authentication check (no userId validation needed)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<WatchlistEntry> entries;

        if (status != null && contentType != null) {
            WatchlistStatus watchlistStatus = WatchlistStatus.valueOf(status.toUpperCase());
            ContentType ct = ContentType.valueOf(contentType.toUpperCase());
            entries = watchlistRepository.findByUserIdAndStatusAndContentType(userId, watchlistStatus, ct, sort);
        } else if (status != null) {
            WatchlistStatus watchlistStatus = WatchlistStatus.valueOf(status.toUpperCase());
            entries = watchlistRepository.findByUserIdAndStatus(userId, watchlistStatus, sort);
        } else if (contentType != null) {
            ContentType ct = ContentType.valueOf(contentType.toUpperCase());
            entries = watchlistRepository.findByUserIdAndContentType(userId, ct, sort);
        } else {
            entries = watchlistRepository.findByUserId(userId, sort);
        }

        return entries.stream()
                .map(entry -> {
                    ContentResponse content = catalogClient.getContentById(entry.getTmdbId(), entry.getContentType().name().toLowerCase());
                    return toResponse(entry, content);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistResponse getEntry(Long userId, Integer tmdbId) {
        WatchlistEntry entry = watchlistRepository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist entry not found"));
        ContentResponse content = catalogClient.getContentById(entry.getTmdbId(), entry.getContentType().name().toLowerCase());
        return toResponse(entry, content);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserAndTmdbId(Long userId, Integer tmdbId) {
        return watchlistRepository.existsByUserIdAndTmdbId(userId, tmdbId);
    }

    @Override
    @Transactional(readOnly = true)
    public WatchlistStatsResponse getStats(Long userId) {
        long total = watchlistRepository.countTotalByUserId(userId);
        long wantToWatch = watchlistRepository.countByUserIdAndStatus(userId, WatchlistStatus.WANT_TO_WATCH);
        long watching = watchlistRepository.countByUserIdAndStatus(userId, WatchlistStatus.WATCHING);
        long completed = watchlistRepository.countByUserIdAndStatus(userId, WatchlistStatus.COMPLETED);
        long totalMovies = watchlistRepository.countByUserIdAndContentType(userId, ContentType.MOVIE);
        long totalSeries = watchlistRepository.countByUserIdAndContentType(userId, ContentType.SERIES);
        return new WatchlistStatsResponse(total, wantToWatch, watching, completed, totalMovies, totalSeries);
    }

    private WatchlistResponse toResponse(WatchlistEntry entry, ContentResponse content) {
        return new WatchlistResponse(
                entry.getId(),
                entry.getTmdbId(),
                entry.getContentType().name(),
                entry.getStatus().name(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                content
        );
    }
}