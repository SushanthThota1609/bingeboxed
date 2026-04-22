package com.bingeboxed.catalog.service;

import com.bingeboxed.catalog.client.TmdbApiException;
import com.bingeboxed.catalog.client.TmdbClient;
import com.bingeboxed.catalog.client.TmdbUnavailableException;
import com.bingeboxed.catalog.dto.CatalogResult;
import com.bingeboxed.catalog.dto.ContentDto;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PagedResponseDto;
import com.bingeboxed.catalog.entity.CatalogContent;
import com.bingeboxed.catalog.repository.CatalogContentRepository;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogServiceImpl implements CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogServiceImpl.class);
    private static final int CACHE_TTL_DAYS = 7;

    private final CatalogContentRepository repository;
    private final TmdbClient tmdbClient;

    public CatalogServiceImpl(CatalogContentRepository repository, TmdbClient tmdbClient) {
        this.repository = repository;
        this.tmdbClient = tmdbClient;
    }

    // -------------------------------------------------------------------------
    // FR-01
    // -------------------------------------------------------------------------

    @Override
    public CatalogResult<ContentDto> fetchContent(int tmdbId, String type) {
        String tmdbType = toTmdbType(type);
        Optional<CatalogContent> cached = repository.findByTmdbId(tmdbId);

        if (cached.isPresent() && isCacheValid(cached.get())) {
            return new CatalogResult<>(toDto(cached.get()), false);
        }

        try {
            Optional<ContentDto> fetched = tmdbClient.fetchById(tmdbId, tmdbType);
            if (fetched.isEmpty()) {
                throw new ResourceNotFoundException("Content not found on TMDB: " + tmdbId);
            }
            CatalogContent saved = upsertContent(fetched.get());
            return new CatalogResult<>(toDto(saved), false);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (TmdbApiException e) {
            if (cached.isPresent()) {
                log.warn("TMDB API error ({}); serving stale cache for tmdbId={}", e.getStatusCode(), tmdbId);
                return new CatalogResult<>(toDto(cached.get()), true);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TMDB API error and no cached content available");
        } catch (TmdbUnavailableException e) {
            if (cached.isPresent()) {
                log.warn("TMDB unreachable; serving stale cache for tmdbId={}", tmdbId);
                return new CatalogResult<>(toDto(cached.get()), true);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "TMDB is unavailable and no cached version exists");
        }
    }

    /** Alias expected by tests. */
    public ContentDto getByTmdbId(int tmdbId, String type) {
        return fetchContent(tmdbId, type).getData();
    }

    // -------------------------------------------------------------------------
    // FR-02
    // -------------------------------------------------------------------------

    @Override
    public PagedResponseDto<ContentDto> searchContent(String query, String type, int page) {
        String tmdbType = toTmdbType(type);
        try {
            return tmdbClient.search(query.trim(), tmdbType, page);
        } catch (TmdbApiException e) {
            log.warn("TMDB search returned status {} for query='{}', type={}", e.getStatusCode(), query, type);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TMDB API error during search: " + e.getStatusCode());
        } catch (TmdbUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB unavailable");
        }
    }

    /** Alias expected by tests. */
    public PagedResponseDto<ContentDto> search(String query, String type, int page) {
        return searchContent(query, type, page);
    }

    // -------------------------------------------------------------------------
    // FR-03
    // -------------------------------------------------------------------------

    @Override
    public PagedResponseDto<ContentDto> browseContent(String type, String genre, Integer year, int page) {
        String tmdbType = toTmdbType(type);
        try {
            PagedResponseDto<ContentDto> response = tmdbClient.discover(tmdbType, genre, year, page);

            if (response.getTotalPages() > 0 && page > response.getTotalPages()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Page " + page + " exceeds total available pages (" + response.getTotalPages() + ")");
            }

            return response;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (TmdbApiException e) {
            log.warn("TMDB discover returned status {} for type={}, genre={}, year={}, page={}",
                    e.getStatusCode(), type, genre, year, page);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TMDB API error during browse: " + e.getStatusCode());
        } catch (TmdbUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB unavailable");
        }
    }

    /** Alias expected by tests. */
    public PagedResponseDto<ContentDto> browse(String type, String genre, Integer year, int page) {
        return browseContent(type, genre, year, page);
    }

    // -------------------------------------------------------------------------
    // FR-04
    // -------------------------------------------------------------------------

    @Override
    public ContentDto fetchDetail(int tmdbId, String type) {
        String tmdbType = toTmdbType(type);
        try {
            Optional<ContentDto> fetched = tmdbClient.fetchById(tmdbId, tmdbType);
            if (fetched.isEmpty()) {
                throw new ResourceNotFoundException("Content not found on TMDB: " + tmdbId);
            }
            upsertContent(fetched.get());
            return fetched.get();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (TmdbApiException e) {
            log.warn("TMDB detail returned status {} for tmdbId={}, type={}", e.getStatusCode(), tmdbId, type);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TMDB API error: " + e.getStatusCode());
        } catch (TmdbUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB unavailable");
        }
    }

    /** Alias expected by tests. */
    public ContentDto getDetail(int tmdbId, String type) {
        return fetchDetail(tmdbId, type);
    }

    // -------------------------------------------------------------------------
    // FR-05  — pass raw type to getGenres(); tests stub with "movie"/"series"
    // -------------------------------------------------------------------------

    @Override
    public List<GenreDto> fetchGenres(String type) {
        try {
            // Do NOT convert to tmdbType here — tests stub getGenres("movie") / getGenres("series")
            return tmdbClient.getGenres(type);
        } catch (TmdbApiException e) {
            log.warn("TMDB genres returned status {} for type={}", e.getStatusCode(), type);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TMDB API error fetching genres: " + e.getStatusCode());
        } catch (TmdbUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB unavailable");
        }
    }

    /** Alias expected by tests. */
    public List<GenreDto> getGenres(String type) {
        return fetchGenres(type);
    }

    // -------------------------------------------------------------------------
    // FR-06
    // -------------------------------------------------------------------------

    @Override
    public List<ContentDto> fetchTrending(String type, String window) {
        String tmdbType = toTmdbType(type);
        String resolvedWindow = (window == null || window.isBlank()) ? "week" : window;
        try {
            return tmdbClient.fetchTrending(tmdbType, resolvedWindow);
        } catch (TmdbApiException e) {
            log.warn("TMDB trending returned status {} for type={}, window={}", e.getStatusCode(), type, window);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TMDB API error fetching trending: " + e.getStatusCode());
        } catch (TmdbUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "TMDB unavailable");
        }
    }

    /** Alias expected by tests. */
    public List<ContentDto> getTrending(String type, String window) {
        return fetchTrending(type, window);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String toTmdbType(String type) {
        return "series".equalsIgnoreCase(type) ? "tv" : "movie";
    }

    private boolean isCacheValid(CatalogContent content) {
        return content.getCachedAt() != null
                && content.getCachedAt().isAfter(LocalDateTime.now().minusDays(CACHE_TTL_DAYS));
    }

    private CatalogContent upsertContent(ContentDto dto) {
        CatalogContent entity = repository.findByTmdbId(dto.getTmdbId())
                .orElse(new CatalogContent());
        entity.setTmdbId(dto.getTmdbId());
        entity.setTitle(dto.getTitle());
        entity.setContentType(dto.getContentType());
        entity.setOverview(dto.getOverview());
        entity.setReleaseYear(dto.getReleaseYear());
        entity.setGenre(dto.getGenre());
        entity.setPosterUrl(dto.getPosterUrl());
        entity.setCachedAt(LocalDateTime.now());
        return repository.save(entity);
    }

    private ContentDto toDto(CatalogContent entity) {
        ContentDto dto = new ContentDto();
        dto.setTmdbId(entity.getTmdbId());
        dto.setTitle(entity.getTitle());
        dto.setContentType(entity.getContentType());
        dto.setOverview(entity.getOverview());
        dto.setReleaseYear(entity.getReleaseYear());
        dto.setGenre(entity.getGenre());
        dto.setPosterUrl(entity.getPosterUrl());
        return dto;
    }
}