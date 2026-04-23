// src/test/java/com/bingeboxed/watchlist/WatchlistServiceTest.java

package com.bingeboxed.watchlist;

import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.shared.client.CatalogClient;
import com.bingeboxed.watchlist.dto.WatchlistResponse;
import com.bingeboxed.watchlist.dto.WatchlistStatsResponse;
import com.bingeboxed.watchlist.entity.WatchlistEntry;
import com.bingeboxed.watchlist.entity.WatchlistEntry.ContentType;
import com.bingeboxed.watchlist.entity.WatchlistEntry.WatchlistStatus;
import com.bingeboxed.watchlist.repository.WatchlistRepository;
import com.bingeboxed.watchlist.service.WatchlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository repository;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private WatchlistServiceImpl watchlistService;

    private static final Long USER_ID = 42L;
    private static final Integer TMDB_ID = 550;

    private WatchlistEntry existingEntry;
    private ContentResponse contentResponse;

    @BeforeEach
    void setUp() {
        existingEntry = new WatchlistEntry();
        existingEntry.setId(1L);
        existingEntry.setUserId(USER_ID);
        existingEntry.setTmdbId(TMDB_ID);
        existingEntry.setContentType(ContentType.MOVIE);
        existingEntry.setStatus(WatchlistStatus.WANT_TO_WATCH);
        existingEntry.setCreatedAt(Instant.now().minusSeconds(3600));
        existingEntry.setUpdatedAt(Instant.now().minusSeconds(3600));

        contentResponse = new ContentResponse(TMDB_ID, "Fight Club", "MOVIE",
                "An insomniac office worker...", 1999, "Drama", "/poster.jpg");
    }

    // --- addToWatchlist ---

    @Test
    void addToWatchlist_savesEntry_andReturnsResponse() {
        when(catalogClient.getContentById(TMDB_ID, "movie")).thenReturn(contentResponse);
        when(repository.save(any())).thenAnswer(inv -> {
            WatchlistEntry e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        WatchlistResponse result = watchlistService.addToWatchlist(USER_ID, TMDB_ID, "MOVIE");

        assertThat(result.getTmdbId()).isEqualTo(TMDB_ID);
        assertThat(result.getStatus()).isEqualTo("WANT_TO_WATCH");
    }

    @Test
    void addToWatchlist_defaultStatusIsWantToWatch() {
        when(catalogClient.getContentById(TMDB_ID, "movie")).thenReturn(contentResponse);
        when(repository.save(any())).thenAnswer(inv -> {
            WatchlistEntry e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        WatchlistResponse result = watchlistService.addToWatchlist(USER_ID, TMDB_ID, "MOVIE");

        assertThat(result.getStatus()).isEqualTo("WANT_TO_WATCH");
    }

    @Test
    void addToWatchlist_throws_whenEntryAlreadyExists() {
        when(catalogClient.getContentById(TMDB_ID, "movie")).thenReturn(contentResponse);
        when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(true);

        assertThatThrownBy(() -> watchlistService.addToWatchlist(USER_ID, TMDB_ID, "MOVIE"))
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void addToWatchlist_throws_whenContentTypeIsInvalid() {
        assertThatThrownBy(() -> watchlistService.addToWatchlist(USER_ID, TMDB_ID, "DOCUMENTARY"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void addToWatchlist_acceptsBothContentTypes() {
        when(catalogClient.getContentById(eq(TMDB_ID), anyString())).thenReturn(contentResponse);
        when(repository.save(any())).thenAnswer(inv -> {
            WatchlistEntry e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        for (String type : List.of("MOVIE", "SERIES")) {
            assertThatNoException().isThrownBy(
                    () -> watchlistService.addToWatchlist(USER_ID, TMDB_ID, type));
        }
    }

    // --- removeFromWatchlist ---

    @Test
    void removeFromWatchlist_deletesEntry_onSuccess() {
        when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID))
                .thenReturn(Optional.of(existingEntry));

        watchlistService.removeFromWatchlist(USER_ID, TMDB_ID);

        verify(repository).delete(existingEntry);
    }

    @Test
    void removeFromWatchlist_throws404_whenEntryNotFound() {
        when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.removeFromWatchlist(USER_ID, TMDB_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void removeFromWatchlist_scopedToUserId_neverCallsBareDeleteById() {
        when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID))
                .thenReturn(Optional.of(existingEntry));

        watchlistService.removeFromWatchlist(USER_ID, TMDB_ID);

        verify(repository).findByUserIdAndTmdbId(USER_ID, TMDB_ID);
        verify(repository, never()).deleteById(any());
    }

    // --- updateStatus ---

    @Test
    void updateStatus_returnsUpdatedEntry() {
        when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID))
                .thenReturn(Optional.of(existingEntry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(catalogClient.getContentById(TMDB_ID, "movie")).thenReturn(contentResponse);

        WatchlistResponse result = watchlistService.updateStatus(USER_ID, TMDB_ID, "COMPLETED");

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void updateStatus_throws404_whenEntryNotFound() {
        when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.updateStatus(USER_ID, TMDB_ID, "WATCHING"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateStatus_throws_whenStatusIsInvalid() {
        assertThatThrownBy(() -> watchlistService.updateStatus(USER_ID, TMDB_ID, "DROPPED"))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void updateStatus_acceptsAllThreeValidStatuses() {
        for (String status : List.of("WANT_TO_WATCH", "WATCHING", "COMPLETED")) {
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID))
                    .thenReturn(Optional.of(existingEntry));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(catalogClient.getContentById(TMDB_ID, "movie")).thenReturn(contentResponse);

            WatchlistResponse result = watchlistService.updateStatus(USER_ID, TMDB_ID, status);
            assertThat(result.getStatus()).isEqualTo(status);
        }
    }

    // --- getUserWatchlist ---

    @Test
    void getUserWatchlist_returnsAllEntries_whenNoFilters() {
        when(repository.findByUserId(eq(USER_ID), any(Sort.class)))
                .thenReturn(List.of(existingEntry));
        when(catalogClient.getContentById(TMDB_ID, "movie")).thenReturn(contentResponse);

        List<WatchlistResponse> result = watchlistService.getUserWatchlist(USER_ID, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserWatchlist_returnsEmptyList_whenNoEntries() {
        when(repository.findByUserId(eq(USER_ID), any(Sort.class))).thenReturn(List.of());

        List<WatchlistResponse> result = watchlistService.getUserWatchlist(USER_ID, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserWatchlist_filtersByStatus_whenProvided() {
        when(repository.findByUserIdAndStatus(eq(USER_ID), eq(WatchlistStatus.WATCHING), any(Sort.class)))
                .thenReturn(List.of());

        watchlistService.getUserWatchlist(USER_ID, "WATCHING", null);

        verify(repository).findByUserIdAndStatus(eq(USER_ID), eq(WatchlistStatus.WATCHING), any(Sort.class));
    }

    @Test
    void getUserWatchlist_filtersByContentType_whenProvided() {
        when(repository.findByUserIdAndContentType(eq(USER_ID), eq(ContentType.SERIES), any(Sort.class)))
                .thenReturn(List.of());

        watchlistService.getUserWatchlist(USER_ID, null, "SERIES");

        verify(repository).findByUserIdAndContentType(eq(USER_ID), eq(ContentType.SERIES), any(Sort.class));
    }

    // --- getEntry ---

    @Test
    void getEntry_returnsEntry_whenFound() {
        when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID))
                .thenReturn(Optional.of(existingEntry));
        when(catalogClient.getContentById(TMDB_ID, "movie")).thenReturn(contentResponse);

        WatchlistResponse result = watchlistService.getEntry(USER_ID, TMDB_ID);

        assertThat(result.getTmdbId()).isEqualTo(TMDB_ID);
    }

    @Test
    void getEntry_throws404_whenNotFound() {
        when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.getEntry(USER_ID, TMDB_ID))
                .isInstanceOf(RuntimeException.class);
    }

    // --- existsByUserAndTmdbId ---

    @Test
    void exists_returnsTrue_whenEntryExists() {
        when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(true);

        assertThat(watchlistService.existsByUserAndTmdbId(USER_ID, TMDB_ID)).isTrue();
    }

    @Test
    void exists_returnsFalse_whenNotFound() {
        when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);

        assertThat(watchlistService.existsByUserAndTmdbId(USER_ID, TMDB_ID)).isFalse();
    }

    // --- getStats ---

    @Test
    void getStats_returnsCorrectCounts() {
        when(repository.countTotalByUserId(USER_ID)).thenReturn(3L);
        when(repository.countByUserIdAndStatus(USER_ID, WatchlistStatus.WANT_TO_WATCH)).thenReturn(1L);
        when(repository.countByUserIdAndStatus(USER_ID, WatchlistStatus.WATCHING)).thenReturn(1L);
        when(repository.countByUserIdAndStatus(USER_ID, WatchlistStatus.COMPLETED)).thenReturn(1L);

        WatchlistStatsResponse stats = watchlistService.getStats(USER_ID);

        assertThat(stats.getTotal()).isEqualTo(3);
        assertThat(stats.getWantToWatch()).isEqualTo(1);
        assertThat(stats.getWatching()).isEqualTo(1);
        assertThat(stats.getCompleted()).isEqualTo(1);
    }

    @Test
    void getStats_allZero_whenWatchlistIsEmpty() {
        when(repository.countTotalByUserId(USER_ID)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(USER_ID, WatchlistStatus.WANT_TO_WATCH)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(USER_ID, WatchlistStatus.WATCHING)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(USER_ID, WatchlistStatus.COMPLETED)).thenReturn(0L);

        WatchlistStatsResponse stats = watchlistService.getStats(USER_ID);

        assertThat(stats.getTotal()).isZero();
        assertThat(stats.getWantToWatch()).isZero();
        assertThat(stats.getWatching()).isZero();
        assertThat(stats.getCompleted()).isZero();
    }
}