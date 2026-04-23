// src/test/java/com/bingeboxed/catalog/CatalogServiceTest.java

package com.bingeboxed.catalog;

import com.bingeboxed.catalog.client.TmdbClient;
import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PaginatedResponse;
import com.bingeboxed.catalog.dto.TrendingItemDto;
import com.bingeboxed.catalog.entity.CatalogContent;
import com.bingeboxed.catalog.exception.TmdbUnavailableException;
import com.bingeboxed.catalog.repository.CatalogContentRepository;
import com.bingeboxed.catalog.service.CatalogServiceImpl;
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
    private ContentResponse tmdbMovieResult;

    @BeforeEach
    void setUp() {
        freshCache = new CatalogContent();
        freshCache.setTmdbId(550);
        freshCache.setTitle("Fight Club");
        freshCache.setContentType("MOVIE");
        freshCache.setGenre("DRAMA");
        freshCache.setCachedAt(LocalDateTime.now().minusDays(1)); // within 7 days

        staleCache = new CatalogContent();
        staleCache.setTmdbId(550);
        staleCache.setTitle("Fight Club");
        staleCache.setContentType("MOVIE");
        staleCache.setCachedAt(LocalDateTime.now().minusDays(10)); // older than 7 days

        tmdbMovieResult = new ContentResponse(550, "Fight Club", "MOVIE",
                "An insomniac office worker...", 1999, "Drama", "/poster.jpg");
    }

    // --- FR-01: Fetch by TMDB ID ---

    @Test
    void getContent_returnsCachedContent_whenCacheIsFresh() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(freshCache));

        ContentResponse result = catalogService.getContentByTmdbId(550, "movie", false);

        assertThat(result.getTmdbId()).isEqualTo(550);
        assertThat(result.getTitle()).isEqualTo("Fight Club");
        verifyNoInteractions(tmdbClient);
    }

    @Test
    void getContent_callsTmdbAndUpserts_whenCacheIsStale() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(staleCache));
        when(tmdbClient.fetchMovieById(550)).thenReturn(tmdbMovieResult);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContentResponse result = catalogService.getContentByTmdbId(550, "movie", false);

        assertThat(result).isNotNull();
        verify(tmdbClient).fetchMovieById(550);
        verify(repository).save(any());
    }

    @Test
    void getContent_callsTmdbAndPersists_whenNotCached() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.empty());
        when(tmdbClient.fetchMovieById(550)).thenReturn(tmdbMovieResult);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContentResponse result = catalogService.getContentByTmdbId(550, "movie", false);

        assertThat(result).isNotNull();
        assertThat(result.getTmdbId()).isEqualTo(550);
        verify(repository).save(any());
    }

    @Test
    void getContent_returnsNull_whenTmdbThrows404() {
        when(repository.findByTmdbId(9999)).thenReturn(Optional.empty());
        when(tmdbClient.fetchMovieById(9999))
                .thenThrow(new com.bingeboxed.catalog.exception.TmdbApiException("Not found", 404));

        ContentResponse result = catalogService.getContentByTmdbId(9999, "movie", false);

        assertThat(result).isNull();
    }

    @Test
    void getContent_usesStaleCacheFallback_whenTmdbIsUnreachable() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(staleCache));
        when(tmdbClient.fetchMovieById(550))
                .thenThrow(new TmdbUnavailableException("Timeout", null));

        ContentResponse result = catalogService.getContentByTmdbId(550, "movie", false);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Fight Club");
        verify(repository, never()).save(any());
    }

    @Test
    void getContent_throwsTmdbUnavailable_whenTmdbDownAndNoCacheExists() {
        when(repository.findByTmdbId(9999)).thenReturn(Optional.empty());
        when(tmdbClient.fetchMovieById(9999))
                .thenThrow(new TmdbUnavailableException("Timeout", null));

        assertThatThrownBy(() -> catalogService.getContentByTmdbId(9999, "movie", false))
                .isInstanceOf(TmdbUnavailableException.class);
    }

    @Test
    void getContent_callsSeriesEndpoint_whenTypeIsSeries() {
        ContentResponse seriesResult = new ContentResponse(1396, "Breaking Bad", "SERIES",
                "A chemistry teacher...", 2008, "Drama", "/bb.jpg");

        when(repository.findByTmdbId(1396)).thenReturn(Optional.empty());
        when(tmdbClient.fetchSeriesById(1396)).thenReturn(seriesResult);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.getContentByTmdbId(1396, "series", false);

        verify(tmdbClient).fetchSeriesById(1396);
        verify(tmdbClient, never()).fetchMovieById(anyInt());
    }

    // --- FR-02: Search ---

    @Test
    void searchContent_returnsPagedResults_forMovieQuery() {
        PaginatedResponse<ContentResponse> page = new PaginatedResponse<>(
                List.of(tmdbMovieResult), 1, 3, 25L);
        when(tmdbClient.searchMovies("fight club", 1)).thenReturn(page);

        PaginatedResponse<ContentResponse> result = catalogService.searchContent("fight club", "movie", 1);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getTotalResults()).isEqualTo(25L);
    }

    @Test
    void searchContent_callsSeriesEndpoint_forSeriesType() {
        PaginatedResponse<ContentResponse> page = new PaginatedResponse<>(List.of(), 1, 0, 0L);
        when(tmdbClient.searchSeries("breaking bad", 1)).thenReturn(page);

        catalogService.searchContent("breaking bad", "series", 1);

        verify(tmdbClient).searchSeries("breaking bad", 1);
        verify(tmdbClient, never()).searchMovies(anyString(), anyInt());
    }

    @Test
    void searchContent_returnsEmptyList_whenNoResultsFound() {
        PaginatedResponse<ContentResponse> empty = new PaginatedResponse<>(List.of(), 1, 0, 0L);
        when(tmdbClient.searchMovies(anyString(), anyInt())).thenReturn(empty);

        PaginatedResponse<ContentResponse> result = catalogService.searchContent("xyzzy123", "movie", 1);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalResults()).isZero();
        // no exception thrown
    }

    @Test
    void searchContent_neverPersistsResults() {
        PaginatedResponse<ContentResponse> page = new PaginatedResponse<>(
                List.of(tmdbMovieResult), 1, 1, 1L);
        when(tmdbClient.searchMovies(anyString(), anyInt())).thenReturn(page);

        catalogService.searchContent("fight club", "movie", 1);

        verifyNoInteractions(repository);
    }

    // --- FR-03: Browse ---

    @Test
    void browseContent_callsDiscoverMovies_withTypeMovie() {
        PaginatedResponse<ContentResponse> page = new PaginatedResponse<>(
                List.of(tmdbMovieResult), 1, 5, 100L);
        when(tmdbClient.discoverMovies(null, null, 1)).thenReturn(page);

        PaginatedResponse<ContentResponse> result = catalogService.browseContent("movie", null, null, 1);

        assertThat(result.getContent()).isNotEmpty();
        verify(tmdbClient).discoverMovies(null, null, 1);
    }

    @Test
    void browseContent_passesYearAndGenre_whenProvided() {
        PaginatedResponse<ContentResponse> page = new PaginatedResponse<>(List.of(), 1, 1, 5L);
        when(tmdbClient.discoverMovies(2023, "28", 1)).thenReturn(page);

        catalogService.browseContent("movie", "28", 2023, 1);

        verify(tmdbClient).discoverMovies(2023, "28", 1);
    }

    @Test
    void browseContent_callsDiscoverSeries_whenTypeIsSeries() {
        PaginatedResponse<ContentResponse> page = new PaginatedResponse<>(List.of(), 1, 1, 0L);
        when(tmdbClient.discoverSeries(null, null, 1)).thenReturn(page);

        catalogService.browseContent("series", null, null, 1);

        verify(tmdbClient).discoverSeries(null, null, 1);
        verify(tmdbClient, never()).discoverMovies(any(), any(), anyInt());
    }

    // --- FR-04: Full Detail (always fresh) ---

    @Test
    void getFullDetail_alwaysCallsTmdb_evenWhenCacheIsFresh() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(freshCache));
        when(tmdbClient.fetchMovieById(550)).thenReturn(tmdbMovieResult);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.getFullDetail(550, "movie");

        // forceRefresh=true means TMDB is always called regardless of cache
        verify(tmdbClient).fetchMovieById(550);
    }

    @Test
    void getFullDetail_upsertsCachedAt_afterFetch() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.of(freshCache));
        when(tmdbClient.fetchMovieById(550)).thenReturn(tmdbMovieResult);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.getFullDetail(550, "movie");

        verify(repository).save(argThat(entity ->
                entity.getCachedAt().isAfter(LocalDateTime.now().minusMinutes(1))
        ));
    }

    @Test
    void getFullDetail_returnsNull_whenTmdbReturns404() {
        when(repository.findByTmdbId(9999)).thenReturn(Optional.empty());
        when(tmdbClient.fetchMovieById(9999))
                .thenThrow(new com.bingeboxed.catalog.exception.TmdbApiException("Not found", 404));

        ContentResponse result = catalogService.getFullDetail(9999, "movie");

        assertThat(result).isNull();
    }

    // --- FR-05: Genres ---

    @Test
    void getGenres_returnsMovieGenres_forMovieType() {
        List<GenreDto> genres = List.of(new GenreDto(28, "Action"), new GenreDto(18, "Drama"));
        when(tmdbClient.getMovieGenres()).thenReturn(genres);

        List<GenreDto> result = catalogService.getGenres("movie");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Action");
        verify(tmdbClient).getMovieGenres();
    }

    @Test
    void getGenres_returnsSeriesGenres_forSeriesType() {
        List<GenreDto> genres = List.of(new GenreDto(10765, "Sci-Fi & Fantasy"));
        when(tmdbClient.getSeriesGenres()).thenReturn(genres);

        List<GenreDto> result = catalogService.getGenres("series");

        assertThat(result).hasSize(1);
        verify(tmdbClient).getSeriesGenres();
        verify(tmdbClient, never()).getMovieGenres();
    }

    // --- FR-06: Trending ---

    @Test
    void getTrending_returnsResults_withDayWindow() {
        TrendingItemDto item = new TrendingItemDto(550, "Fight Club", "movie", "/p.jpg", 1999, null);
        when(tmdbClient.getTrending("movie", "day")).thenReturn(List.of(item));

        List<TrendingItemDto> result = catalogService.getTrending("movie", "day");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Fight Club");
    }

    @Test
    void getTrending_passesWeekWindow_whenExplicitlySet() {
        when(tmdbClient.getTrending("movie", "week")).thenReturn(List.of());

        catalogService.getTrending("movie", "week");

        verify(tmdbClient).getTrending("movie", "week");
    }

    @Test
    void getTrending_workForSeriesType() {
        when(tmdbClient.getTrending("series", "week")).thenReturn(List.of());

        catalogService.getTrending("series", "week");

        verify(tmdbClient).getTrending("series", "week");
    }

    // --- NFR-10: Uppercase normalization on persistence ---

    @Test
    void persistedContent_normalizesContentTypeAndGenreToUppercase() {
        when(repository.findByTmdbId(550)).thenReturn(Optional.empty());

        // TMDB returns mixed case — service must normalize before saving
        ContentResponse mixedCase = new ContentResponse(550, "Fight Club", "movie",
                "...", 1999, "drama", "/p.jpg");
        when(tmdbClient.fetchMovieById(550)).thenReturn(mixedCase);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        catalogService.getContentByTmdbId(550, "movie", false);

        verify(repository).save(argThat(entity ->
                "MOVIE".equals(entity.getContentType()) &&
                        "DRAMA".equals(entity.getGenre())
        ));
    }
}