// src/main/java/com/bingeboxed/watchlist/service/WatchlistServiceImpl.java
package com.bingeboxed.watchlist.service;

import com.bingeboxed.shared.client.CatalogClient;
import com.bingeboxed.shared.client.CatalogClientException;
import com.bingeboxed.shared.client.CatalogContentDto;
import com.bingeboxed.shared.client.CatalogUnavailableException;
import com.bingeboxed.watchlist.dto.AddToWatchlistRequest;
import com.bingeboxed.watchlist.dto.ContainsResponse;
import com.bingeboxed.watchlist.dto.UpdateStatusRequest;
import com.bingeboxed.watchlist.dto.WatchlistEntryResponse;
import com.bingeboxed.watchlist.dto.WatchlistStatsResponse;
import com.bingeboxed.watchlist.entity.WatchlistEntry;
import com.bingeboxed.watchlist.repository.WatchlistRepository;
import com.bingeboxed.watchlist.repository.dto.StatusCount;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WatchlistServiceImpl implements WatchlistService {

    private static final Set<String> VALID_CONTENT_TYPES = Set.of("MOVIE", "SERIES");
    private static final Set<String> VALID_STATUSES      = Set.of("WANT_TO_WATCH", "WATCHING", "COMPLETED");
    private static final String      DEFAULT_STATUS       = "WANT_TO_WATCH";

    private final WatchlistRepository repository;
    private final CatalogClient       catalogClient;

    public WatchlistServiceImpl(WatchlistRepository repository, CatalogClient catalogClient) {
        this.repository    = repository;
        this.catalogClient = catalogClient;
    }

    // -------------------------------------------------------------------------
    // FR-01
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public WatchlistEntryResponse add(Long userId, AddToWatchlistRequest request) {
        if (request.getTmdbId() == null || request.getTmdbId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tmdbId must be a positive integer");
        }
        if (request.getContentType() == null || !VALID_CONTENT_TYPES.contains(request.getContentType().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "contentType must be MOVIE or SERIES");
        }

        String contentType = request.getContentType().toUpperCase();

        // Application-level duplicate check (NFR-04)
        if (repository.existsByUserIdAndTmdbId(userId, request.getTmdbId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Entry already exists in watchlist");
        }

        // Verify content exists in catalog (FR-01)
        CatalogContentDto content = fetchContentOrThrow(request.getTmdbId(), contentType);

        WatchlistEntry entry = new WatchlistEntry();
        entry.setUserId(userId);
        entry.setTmdbId(request.getTmdbId());
        entry.setContentType(contentType);
        entry.setStatus(DEFAULT_STATUS);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        try {
            entry = repository.save(entry);
        } catch (DataIntegrityViolationException e) {
            // Database-level duplicate (NFR-04)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Entry already exists in watchlist");
        }

        return toResponse(entry, content);
    }

    // -------------------------------------------------------------------------
    // FR-02
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void remove(Long userId, Integer tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tmdbId must be a positive integer");
        }
        WatchlistEntry entry = findEntryOrThrow(userId, tmdbId);
        repository.delete(entry);
    }

    // -------------------------------------------------------------------------
    // FR-03
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public WatchlistEntryResponse updateStatus(Long userId, Integer tmdbId, UpdateStatusRequest request) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tmdbId must be a positive integer");
        }
        if (request.getStatus() == null || !VALID_STATUSES.contains(request.getStatus().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status must be WANT_TO_WATCH, WATCHING, or COMPLETED");
        }

        WatchlistEntry entry = findEntryOrThrow(userId, tmdbId);
        entry.setStatus(request.getStatus().toUpperCase());
        entry.setUpdatedAt(LocalDateTime.now());
        entry = repository.save(entry);

        CatalogContentDto content = fetchContentSilently(tmdbId, entry.getContentType());
        return toResponse(entry, content);
    }

    // -------------------------------------------------------------------------
    // FR-04
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistEntryResponse> getWatchlist(Long userId, String status, String type) {
        validateFilters(status, type);
        List<WatchlistEntry> entries = queryEntries(userId, status, type);
        return enrich(entries);
    }

    // -------------------------------------------------------------------------
    // FR-05
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public WatchlistEntryResponse getEntry(Long userId, Integer tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tmdbId must be a positive integer");
        }
        WatchlistEntry entry = findEntryOrThrow(userId, tmdbId);
        CatalogContentDto content = fetchContentSilently(tmdbId, entry.getContentType());
        return toResponse(entry, content);
    }

    // -------------------------------------------------------------------------
    // FR-06
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<WatchlistEntryResponse> getPublicWatchlist(Long userId, String status, String type) {
        validateFilters(status, type);
        List<WatchlistEntry> entries = queryEntries(userId, status, type);
        return enrich(entries);
    }

    // -------------------------------------------------------------------------
    // FR-07
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public WatchlistStatsResponse getStats(Long userId) {
        List<StatusCount> counts = repository.countByStatusForUser(userId);

        long wantToWatch = 0, watching = 0, completed = 0;
        for (StatusCount sc : counts) {
            switch (sc.getStatus()) {
                case "WANT_TO_WATCH" -> wantToWatch = sc.getCount();
                case "WATCHING"      -> watching    = sc.getCount();
                case "COMPLETED"     -> completed   = sc.getCount();
            }
        }

        long totalMovies = repository.countByUserIdAndContentType(userId, "MOVIE");
        long totalSeries = repository.countByUserIdAndContentType(userId, "SERIES");

        WatchlistStatsResponse stats = new WatchlistStatsResponse();
        stats.setTotal(wantToWatch + watching + completed);
        stats.setWantToWatch(wantToWatch);
        stats.setWatching(watching);
        stats.setCompleted(completed);
        stats.setTotalMovies(totalMovies);
        stats.setTotalSeries(totalSeries);
        return stats;
    }

    // -------------------------------------------------------------------------
    // FR-08
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ContainsResponse contains(Long userId, Integer tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tmdbId must be a positive integer");
        }
        Optional<WatchlistEntry> entry = repository.findByUserIdAndTmdbId(userId, tmdbId);
        if (entry.isPresent()) {
            return new ContainsResponse(true, entry.get().getStatus());
        }
        return new ContainsResponse(false, null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private WatchlistEntry findEntryOrThrow(Long userId, Integer tmdbId) {
        return repository.findByUserIdAndTmdbId(userId, tmdbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Watchlist entry not found"));
    }

    private CatalogContentDto fetchContentOrThrow(Integer tmdbId, String contentType) {
        try {
            return catalogClient.findById(tmdbId, contentType)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Content not found in catalog"));
        } catch (CatalogClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Catalog service is currently unavailable");
        }
    }

    /** Best-effort enrichment — returns null on any failure so callers get partial data. */
    private CatalogContentDto fetchContentSilently(Integer tmdbId, String contentType) {
        try {
            return catalogClient.findById(tmdbId, contentType).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private List<WatchlistEntryResponse> enrich(List<WatchlistEntry> entries) {
        return entries.stream()
                .map(e -> toResponse(e, fetchContentSilently(e.getTmdbId(), e.getContentType())))
                .collect(Collectors.toList());
    }

    private List<WatchlistEntry> queryEntries(Long userId, String status, String type) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasType   = type   != null && !type.isBlank();

        if (hasStatus && hasType) {
            return repository.findByUserIdAndStatusAndContentTypeOrderByCreatedAtDesc(
                    userId, status.toUpperCase(), type.toUpperCase());
        }
        if (hasStatus) {
            return repository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, status.toUpperCase());
        }
        if (hasType) {
            return repository.findByUserIdAndContentTypeOrderByCreatedAtDesc(
                    userId, type.toUpperCase());
        }
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void validateFilters(String status, String type) {
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status filter must be WANT_TO_WATCH, WATCHING, or COMPLETED");
        }
        if (type != null && !type.isBlank() && !VALID_CONTENT_TYPES.contains(type.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "type filter must be MOVIE or SERIES");
        }
    }

    private WatchlistEntryResponse toResponse(WatchlistEntry entry, CatalogContentDto content) {
        WatchlistEntryResponse r = new WatchlistEntryResponse();
        r.setTmdbId(entry.getTmdbId());
        r.setContentType(entry.getContentType());
        r.setStatus(entry.getStatus());

        if (content != null) {
            r.setTitle(content.getTitle());
            r.setReleaseYear(content.getReleaseYear());
            r.setGenre(content.getGenre());
            r.setPosterUrl(content.getPosterUrl());
            r.setOverview(content.getOverview());
        } else {
            r.setTitle("Unknown Title");
        }
        return r;
    }
}