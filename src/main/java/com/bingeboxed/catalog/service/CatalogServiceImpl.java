package com.bingeboxed.catalog.service;

import com.bingeboxed.catalog.client.TmdbClient;
import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PaginatedResponse;
import com.bingeboxed.catalog.dto.TrendingItemDto;
import com.bingeboxed.catalog.entity.CatalogContent;
import com.bingeboxed.catalog.exception.TmdbApiException;
import com.bingeboxed.catalog.exception.TmdbUnavailableException;
import com.bingeboxed.catalog.repository.CatalogContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogServiceImpl implements CatalogService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogServiceImpl.class);
    private static final int CACHE_DAYS = 7;

    private final TmdbClient tmdbClient;
    private final CatalogContentRepository repository;

    public CatalogServiceImpl(TmdbClient tmdbClient, CatalogContentRepository repository) {
        this.tmdbClient = tmdbClient;
        this.repository = repository;
    }

    @Override
    @Transactional
    public ContentResponse getContentByTmdbId(Integer tmdbId, String type, boolean forceRefresh) {
        Optional<CatalogContent> cached = repository.findByTmdbId(tmdbId);
        boolean cacheValid = cached.isPresent() && !isCacheExpired(cached.get()) && !forceRefresh;

        if (cacheValid) {
            return toResponse(cached.get());
        }

        // Fetch from TMDB
        try {
            ContentResponse fresh = fetchFromTmdb(tmdbId, type);
            upsertCatalogContent(tmdbId, fresh, type);
            return fresh;
        } catch (TmdbUnavailableException e) {
            // Fallback to cached if exists, regardless of age
            if (cached.isPresent()) {
                logger.warn("TMDB unavailable, returning stale cache for tmdbId={}", tmdbId);
                return toResponse(cached.get());
            }
            throw e;
        } catch (TmdbApiException e) {
            if (e.getStatusCode() == 404) {
                return null; // Not found in TMDB
            }
            throw e;
        }
    }

    @Override
    public PaginatedResponse<ContentResponse> searchContent(String query, String type, int page) {
        if ("movie".equals(type)) {
            return tmdbClient.searchMovies(query, page);
        } else {
            return tmdbClient.searchSeries(query, page);
        }
    }

    @Override
    public PaginatedResponse<ContentResponse> browseContent(String type, String genre, Integer year, int page) {
        // Map genre name to id? TMDB expects genre ids. We'll need to convert genre name to id if provided.
        // For simplicity, we assume genre is the genre id as string. In real UI, we'd fetch genre list and pass id.
        // Here we pass genre as is.
        if ("movie".equals(type)) {
            return tmdbClient.discoverMovies(year, genre, page);
        } else {
            return tmdbClient.discoverSeries(year, genre, page);
        }
    }

    @Override
    @Transactional
    public ContentResponse getFullDetail(Integer tmdbId, String type) {
        return getContentByTmdbId(tmdbId, type, true);
    }

    @Override
    public List<GenreDto> getGenres(String type) {
        if ("movie".equals(type)) {
            return tmdbClient.getMovieGenres();
        } else {
            return tmdbClient.getSeriesGenres();
        }
    }

    @Override
    public List<TrendingItemDto> getTrending(String type, String window) {
        return tmdbClient.getTrending(type, window);
    }

    private ContentResponse fetchFromTmdb(Integer tmdbId, String type) {
        if ("movie".equals(type)) {
            return tmdbClient.fetchMovieById(tmdbId);
        } else {
            return tmdbClient.fetchSeriesById(tmdbId);
        }
    }

    private void upsertCatalogContent(Integer tmdbId, ContentResponse dto, String type) {
        Optional<CatalogContent> existing = repository.findByTmdbId(tmdbId);
        CatalogContent entity = existing.orElse(new CatalogContent());
        entity.setTmdbId(tmdbId);
        entity.setTitle(dto.getTitle());
        entity.setContentType(type.toUpperCase());
        entity.setOverview(dto.getOverview());
        entity.setReleaseYear(dto.getReleaseYear());
        // Normalize genre to uppercase
        String genre = dto.getGenre();
        if (genre != null) {
            entity.setGenre(genre.toUpperCase());
        } else {
            entity.setGenre(null);
        }
        entity.setPosterUrl(dto.getPosterUrl());
        entity.setCachedAt(LocalDateTime.now());
        repository.save(entity);
    }

    private ContentResponse toResponse(CatalogContent entity) {
        return new ContentResponse(
                entity.getTmdbId(),
                entity.getTitle(),
                entity.getContentType(),
                entity.getOverview(),
                entity.getReleaseYear(),
                entity.getGenre(),
                entity.getPosterUrl()
        );
    }

    private boolean isCacheExpired(CatalogContent entity) {
        if (entity.getCachedAt() == null) return true;
        return entity.getCachedAt().plusDays(CACHE_DAYS).isBefore(LocalDateTime.now());
    }
}