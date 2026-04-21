// src/test/java/com/bingeboxed/catalog/CatalogServiceTest.java

package com.bingeboxed.catalog;

import com.bingeboxed.catalog.client.TmdbClient;
import com.bingeboxed.catalog.dto.ContentDto;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PagedResponseDto;
import com.bingeboxed.catalog.entity.CatalogContent;
import com.bingeboxed.catalog.repository.CatalogContentRepository;
import com.bingeboxed.catalog.service.CatalogServiceImpl;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogContentRepository repository;

    @Mock
    private TmdbClient tmdbClient;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private CatalogContent freshCache;
    private CatalogContent staleCache;
    private ContentDto tmdbResult;

    @BeforeEach
    void setUp() {
        freshCache = new CatalogContent();
        freshCache.setTmdbId(550);
        freshCache.setTitle("Fight Club");
        freshCache.setContentType("MOVIE");
        freshCache.setCachedAt(LocalDateTime.now().minusDays(1));

        staleCache = new CatalogContent();
        staleCache.setTmdbId(550);
        staleCache.setTitle("Fight Club");
        staleCache.setContentType("MOVIE");
        staleCache.setCachedAt(LocalDateTime.now().minusDays(10));

        tmdbResult = new ContentDto();
        tmdbResult.setTmdbId(550);
        tmdbResult.setTitle("Fight Club");
        tmdbResult.setContentType("MOVIE");
        tmdbResult.setReleaseYear(1999);
    }

    // ─── FR-01: Fetch by TMDB ID ──────────────────────────────────────────────

    @Test
    void fetchById_returnsCachedContent_whenCacheIsFresh() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(freshCache));

        ContentDto result = catalogService.getByTmdbId(550, "movie");

        assertThat(result.getTmdbId()).isEqualTo(550);
        verify(tmdbClient, never()).fetchById(anyInt(), anyString());
    }

    @Test
    void fetchById_callsTmdb_whenCacheIsStale() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(staleCache));
        when(tmdbClient.fetchById(550, "movie")).thenReturn(Optional.of(tmdbResult));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.getByTmdbId(550, "movie");

        verify(tmdbClient).fetchById(550, "movie");
        verify(repository).save(any());
    }

    @Test
    void fetchById_callsTmdb_whenNotCached() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.empty());
        when(tmdbClient.fetchById(550, "movie")).thenReturn(Optional.of(tmdbResult));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContentDto result = catalogService.getByTmdbId(550, "movie");

        assertThat(result).isNotNull();
        verify(tmdbClient).fetchById(550, "movie");
    }

    @Test
    void fetchById_throws404_whenNotFoundInTmdb() {
        when(repository.findByTmdbId(9999)).thenReturn(Optional.empty());
        when(tmdbClient.fetchById(9999, "movie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getByTmdbId(9999, "movie"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── FR-02: Search ────────────────────────────────────────────────────────

    @Test
    void search_returnsPagedResults_onValidQuery() {
        PagedResponseDto<ContentDto> page = new PagedResponseDto<>();
        page.setContent(List.of(tmdbResult));
        page.setPage(1);
        page.setTotalPages(3);
        page.setTotalResults(25);

        when(tmdbClient.search("fight club", "movie", 1)).thenReturn(page);

        PagedResponseDto<ContentDto> result = catalogService.search("fight club", "movie", 1);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void search_trimsQuery_beforeCallingTmdb() {
        PagedResponseDto<ContentDto> page = new PagedResponseDto<>();
        page.setContent(List.of());
        when(tmdbClient.search("fight club", "movie", 1)).thenReturn(page);

        catalogService.search("  fight club  ", "movie", 1);

        verify(tmdbClient).search("fight club", "movie", 1);
    }

    @Test
    void search_returnsEmptyList_whenNoResultsFound() {
        PagedResponseDto<ContentDto> empty = new PagedResponseDto<>();
        empty.setContent(List.of());
        empty.setTotalResults(0);
        when(tmdbClient.search(anyString(), anyString(), anyInt())).thenReturn(empty);

        PagedResponseDto<ContentDto> result = catalogService.search("xyzzy123", "movie", 1);

        assertThat(result.getContent()).isEmpty();
        // should NOT throw a 404
    }

    @Test
    void search_doesNotPersistResults() {
        PagedResponseDto<ContentDto> page = new PagedResponseDto<>();
        page.setContent(List.of(tmdbResult));
        when(tmdbClient.search(anyString(), anyString(), anyInt())).thenReturn(page);

        catalogService.search("fight club", "movie", 1);

        verifyNoInteractions(repository);
    }

    // ─── FR-03: Browse ────────────────────────────────────────────────────────

    @Test
    void browse_returnsResults_withTypeOnly() {
        PagedResponseDto<ContentDto> page = new PagedResponseDto<>();
        page.setContent(List.of(tmdbResult));
        page.setPage(1);
        page.setTotalPages(5);
        page.setTotalResults(100);

        when(tmdbClient.discover("movie", null, null, 1)).thenReturn(page);

        PagedResponseDto<ContentDto> result = catalogService.browse("movie", null, null, 1);

        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    void browse_passesFiltersToTmdb_whenProvided() {
        PagedResponseDto<ContentDto> page = new PagedResponseDto<>();
        page.setContent(List.of());
        when(tmdbClient.discover("movie", "28", 2024, 1)).thenReturn(page);

        catalogService.browse("movie", "28", 2024, 1);

        verify(tmdbClient).discover("movie", "28", 2024, 1);
    }

    // ─── FR-04: Full Detail (always fresh) ───────────────────────────────────

    @Test
    void getDetail_alwaysCallsTmdb_evenWhenCacheIsFresh() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(freshCache));
        when(tmdbClient.fetchById(550, "movie")).thenReturn(Optional.of(tmdbResult));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.getDetail(550, "movie");

        verify(tmdbClient).fetchById(550, "movie");
    }

    @Test
    void getDetail_upsertsRecord_afterFetch() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(freshCache));
        when(tmdbClient.fetchById(550, "movie")).thenReturn(Optional.of(tmdbResult));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.getDetail(550, "movie");

        verify(repository).save(any());
    }

    // ─── FR-05: Genres ────────────────────────────────────────────────────────

    @Test
    void getGenres_returnsGenreList_forMovie() {
        List<GenreDto> genres = List.of(new GenreDto(28, "Action"), new GenreDto(18, "Drama"));
        when(tmdbClient.getGenres("movie")).thenReturn(genres);

        List<GenreDto> result = catalogService.getGenres("movie");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Action");
    }

    @Test
    void getGenres_returnsGenreList_forSeries() {
        List<GenreDto> genres = List.of(new GenreDto(10765, "Sci-Fi & Fantasy"));
        when(tmdbClient.getGenres("series")).thenReturn(genres);

        List<GenreDto> result = catalogService.getGenres("series");

        assertThat(result).hasSize(1);
    }

    // ─── FR-06: Trending ─────────────────────────────────────────────────────

    @Test
    void getTrending_returnsResults_withExplicitDayWindow() {
        when(tmdbClient.fetchTrending("movie", "day")).thenReturn(List.of(tmdbResult));

        List<ContentDto> result = catalogService.getTrending("movie", "day");

        assertThat(result).hasSize(1);
    }

    @Test
    void getTrending_defaultsToWeek_whenWindowIsNull() {
        when(tmdbClient.fetchTrending("movie", "week")).thenReturn(List.of(tmdbResult));

        catalogService.getTrending("movie", null);

        verify(tmdbClient).fetchTrending("movie", "week");
    }
}