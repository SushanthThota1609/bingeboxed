package com.bingeboxed.watchlist;

import com.bingeboxed.shared.client.CatalogClient;
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
import com.bingeboxed.watchlist.service.WatchlistServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistService Tests")
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository repository;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private WatchlistServiceImpl service;

    // ─── Shared test fixtures ────────────────────────────────────────────────

    private static final Long    USER_ID   = 1L;
    private static final Integer TMDB_ID   = 550;
    private static final String  MOVIE     = "MOVIE";
    private static final String  SERIES    = "SERIES";

    private WatchlistEntry buildEntry(Long userId, Integer tmdbId, String contentType, String status) {
        WatchlistEntry e = new WatchlistEntry();
        e.setId(1L);
        e.setUserId(userId);
        e.setTmdbId(tmdbId);
        e.setContentType(contentType);
        e.setStatus(status);
        e.setCreatedAt(LocalDateTime.now().minusDays(1));
        e.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return e;
    }

    private CatalogContentDto buildCatalogDto(String contentType) {
        CatalogContentDto dto = new CatalogContentDto();
        dto.setTmdbId(TMDB_ID);
        dto.setTitle("Fight Club");
        dto.setContentType(contentType);
        dto.setOverview("An insomniac office worker forms a fight club.");
        dto.setReleaseYear(1999);
        dto.setGenre("Drama");
        dto.setPosterUrl("https://image.tmdb.org/t/p/w500/poster.jpg");
        return dto;
    }

    private AddToWatchlistRequest buildAddRequest(Integer tmdbId, String contentType) {
        AddToWatchlistRequest req = new AddToWatchlistRequest();
        req.setTmdbId(tmdbId);
        req.setContentType(contentType);
        return req;
    }

    private UpdateStatusRequest buildUpdateRequest(String status) {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus(status);
        return req;
    }

    // =========================================================================
    // FR-01: Add to Watchlist
    // =========================================================================

    @Nested
    @DisplayName("FR-01: add()")
    class AddTests {

        @Test
        @DisplayName("Happy path — new MOVIE entry is saved with WANT_TO_WATCH status and enriched response returned")
        void add_newMovie_returnsEnrichedResponse() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            WatchlistEntryResponse result = service.add(USER_ID, req);

            assertThat(result.getTmdbId()).isEqualTo(TMDB_ID);
            assertThat(result.getContentType()).isEqualTo(MOVIE);
            assertThat(result.getStatus()).isEqualTo("WANT_TO_WATCH");
            assertThat(result.getTitle()).isEqualTo("Fight Club");
            assertThat(result.getReleaseYear()).isEqualTo(1999);
            assertThat(result.getPosterUrl()).isNotBlank();
        }

        @Test
        @DisplayName("Happy path — new SERIES entry is saved correctly")
        void add_newSeries_savedWithCorrectContentType() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, SERIES);
            CatalogContentDto catalog = buildCatalogDto(SERIES);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, SERIES, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, SERIES)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            WatchlistEntryResponse result = service.add(USER_ID, req);

            assertThat(result.getContentType()).isEqualTo(SERIES);
        }

        @Test
        @DisplayName("Default status — newly added entry always has status WANT_TO_WATCH")
        void add_newEntry_defaultStatusIsWantToWatch() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            WatchlistEntryResponse result = service.add(USER_ID, req);

            assertThat(result.getStatus()).isEqualTo("WANT_TO_WATCH");
        }

        @Test
        @DisplayName("Duplicate check — 409 CONFLICT thrown when entry already exists (app-level check)")
        void add_duplicateEntry_appLevelCheck_throwsConflict() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(catalogClient, never()).findById(anyInt(), anyString());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Duplicate check — 409 CONFLICT thrown when DB constraint fires (race condition)")
        void add_duplicateEntry_dbLevelConstraint_throwsConflict() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Catalog validation — 404 NOT_FOUND thrown when tmdbId not in catalog")
        void add_contentNotInCatalog_throws404() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Catalog unavailable — 503 SERVICE_UNAVAILABLE thrown when catalog is unreachable")
        void add_catalogUnavailable_throws503() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE))
                    .thenThrow(new CatalogUnavailableException("unreachable", new RuntimeException()));

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Validation — null tmdbId throws 400 BAD_REQUEST")
        void add_nullTmdbId_throws400() {
            AddToWatchlistRequest req = buildAddRequest(null, MOVIE);

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — zero tmdbId throws 400 BAD_REQUEST")
        void add_zeroTmdbId_throws400() {
            AddToWatchlistRequest req = buildAddRequest(0, MOVIE);

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — negative tmdbId throws 400 BAD_REQUEST")
        void add_negativeTmdbId_throws400() {
            AddToWatchlistRequest req = buildAddRequest(-1, MOVIE);

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — null contentType throws 400 BAD_REQUEST")
        void add_nullContentType_throws400() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, null);

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — invalid contentType throws 400 BAD_REQUEST")
        void add_invalidContentType_throws400() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, "DOCUMENTARY");

            assertThatThrownBy(() -> service.add(USER_ID, req))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Case normalization — lowercase contentType 'movie' is accepted and stored as 'MOVIE'")
        void add_lowercaseContentType_isNormalized() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, "movie");
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(eq(TMDB_ID), anyString())).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            WatchlistEntryResponse result = service.add(USER_ID, req);

            assertThat(result.getContentType()).isEqualTo(MOVIE);
        }

        @Test
        @DisplayName("Save interaction — repository.save() is called exactly once on success")
        void add_success_saveCalledOnce() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            service.add(USER_ID, req);

            verify(repository, times(1)).save(any(WatchlistEntry.class));
        }
    }

    // =========================================================================
    // FR-02: Remove from Watchlist
    // =========================================================================

    @Nested
    @DisplayName("FR-02: remove()")
    class RemoveTests {

        @Test
        @DisplayName("Happy path — existing entry is deleted successfully")
        void remove_existingEntry_deletesFromRepository() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));

            service.remove(USER_ID, TMDB_ID);

            verify(repository, times(1)).delete(entry);
        }

        @Test
        @DisplayName("Not found — 404 thrown when entry does not exist for this user")
        void remove_entryNotFound_throws404() {
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.remove(USER_ID, TMDB_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("Isolation — user cannot delete another user's entry (entry not found for their userId)")
        void remove_anotherUsersEntry_throws404() {
            Long otherUserId = 99L;
            when(repository.findByUserIdAndTmdbId(otherUserId, TMDB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.remove(otherUserId, TMDB_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Validation — null tmdbId throws 400 BAD_REQUEST")
        void remove_nullTmdbId_throws400() {
            assertThatThrownBy(() -> service.remove(USER_ID, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — non-positive tmdbId throws 400 BAD_REQUEST")
        void remove_nonPositiveTmdbId_throws400() {
            assertThatThrownBy(() -> service.remove(USER_ID, 0))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // =========================================================================
    // FR-03: Update Status
    // =========================================================================

    @Nested
    @DisplayName("FR-03: updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("Happy path — status updated to WATCHING and enriched response returned")
        void updateStatus_toWatching_updatesAndReturnsResponse() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WATCHING");
            CatalogContentDto catalog = buildCatalogDto(MOVIE);

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));

            WatchlistEntryResponse result = service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest("WATCHING"));

            assertThat(result.getStatus()).isEqualTo("WATCHING");
            assertThat(result.getTitle()).isEqualTo("Fight Club");
        }

        @Test
        @DisplayName("Happy path — status updated to COMPLETED")
        void updateStatus_toCompleted_updatesSuccessfully() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WATCHING");
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "COMPLETED");

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.empty());

            WatchlistEntryResponse result = service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest("COMPLETED"));

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Happy path — status updated back to WANT_TO_WATCH")
        void updateStatus_toWantToWatch_updatesSuccessfully() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "COMPLETED");
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.empty());

            WatchlistEntryResponse result = service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest("WANT_TO_WATCH"));

            assertThat(result.getStatus()).isEqualTo("WANT_TO_WATCH");
        }

        @Test
        @DisplayName("Not found — 404 thrown when entry does not exist")
        void updateStatus_entryNotFound_throws404() {
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest("WATCHING")))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Validation — invalid status value throws 400 BAD_REQUEST")
        void updateStatus_invalidStatus_throws400() {
            assertThatThrownBy(() -> service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest("PAUSED")))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — null status throws 400 BAD_REQUEST")
        void updateStatus_nullStatus_throws400() {
            assertThatThrownBy(() -> service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest(null)))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — non-positive tmdbId throws 400 BAD_REQUEST")
        void updateStatus_invalidTmdbId_throws400() {
            assertThatThrownBy(() -> service.updateStatus(USER_ID, 0, buildUpdateRequest("WATCHING")))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Case normalization — lowercase 'watching' is accepted")
        void updateStatus_lowercaseStatus_isNormalized() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WATCHING");

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.empty());

            WatchlistEntryResponse result = service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest("watching"));

            assertThat(result.getStatus()).isEqualTo("WATCHING");
        }

        @Test
        @DisplayName("Catalog degraded — catalog unavailable during update still saves and returns partial data")
        void updateStatus_catalogUnavailable_stillSavesAndReturnsPartialResponse() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WATCHING");

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);
            when(catalogClient.findById(TMDB_ID, MOVIE))
                    .thenThrow(new CatalogUnavailableException("down", new RuntimeException()));

            WatchlistEntryResponse result = service.updateStatus(USER_ID, TMDB_ID, buildUpdateRequest("WATCHING"));

            assertThat(result.getStatus()).isEqualTo("WATCHING");
            assertThat(result.getTitle()).isEqualTo("Unknown Title");
        }
    }

    // =========================================================================
    // FR-04: Get Watchlist (with filters)
    // =========================================================================

    @Nested
    @DisplayName("FR-04: getWatchlist()")
    class GetWatchlistTests {

        @Test
        @DisplayName("No filters — returns all entries for the user ordered by createdAt DESC")
        void getWatchlist_noFilters_returnsAllEntries() {
            List<WatchlistEntry> entries = List.of(
                    buildEntry(USER_ID, 550, MOVIE, "WANT_TO_WATCH"),
                    buildEntry(USER_ID, 1399, SERIES, "WATCHING")
            );
            when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(entries);
            when(catalogClient.findById(anyInt(), anyString())).thenReturn(Optional.empty());

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, null, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Filter by status — returns only WATCHING entries")
        void getWatchlist_filterByStatus_returnsMatchingEntries() {
            List<WatchlistEntry> entries = List.of(
                    buildEntry(USER_ID, 550, MOVIE, "WATCHING")
            );
            when(repository.findByUserIdAndStatusOrderByCreatedAtDesc(USER_ID, "WATCHING"))
                    .thenReturn(entries);
            when(catalogClient.findById(anyInt(), anyString())).thenReturn(Optional.empty());

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, "WATCHING", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("WATCHING");
        }

        @Test
        @DisplayName("Filter by type — returns only MOVIE entries")
        void getWatchlist_filterByType_returnsMatchingEntries() {
            List<WatchlistEntry> entries = List.of(
                    buildEntry(USER_ID, 550, MOVIE, "WANT_TO_WATCH")
            );
            when(repository.findByUserIdAndContentTypeOrderByCreatedAtDesc(USER_ID, MOVIE))
                    .thenReturn(entries);
            when(catalogClient.findById(anyInt(), anyString())).thenReturn(Optional.empty());

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, null, MOVIE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContentType()).isEqualTo(MOVIE);
        }

        @Test
        @DisplayName("Filter by status + type — returns only COMPLETED SERIES entries")
        void getWatchlist_filterByStatusAndType_returnsMatchingEntries() {
            List<WatchlistEntry> entries = List.of(
                    buildEntry(USER_ID, 1399, SERIES, "COMPLETED")
            );
            when(repository.findByUserIdAndStatusAndContentTypeOrderByCreatedAtDesc(USER_ID, "COMPLETED", SERIES))
                    .thenReturn(entries);
            when(catalogClient.findById(anyInt(), anyString())).thenReturn(Optional.empty());

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, "COMPLETED", SERIES);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("COMPLETED");
            assertThat(result.get(0).getContentType()).isEqualTo(SERIES);
        }

        @Test
        @DisplayName("Empty watchlist — returns empty list, not null")
        void getWatchlist_noEntries_returnsEmptyList() {
            when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Collections.emptyList());

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, null, null);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Enrichment — catalog metadata merged into each response item")
        void getWatchlist_catalogAvailable_entriesEnrichedWithMetadata() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");
            CatalogContentDto catalog = buildCatalogDto(MOVIE);

            when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(entry));
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, null, null);

            assertThat(result.get(0).getTitle()).isEqualTo("Fight Club");
            assertThat(result.get(0).getPosterUrl()).isNotBlank();
        }

        @Test
        @DisplayName("Enrichment degraded — catalog failure returns 'Unknown Title' for affected entries")
        void getWatchlist_catalogUnavailable_returnsPartialDataWithUnknownTitle() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(entry));
            when(catalogClient.findById(TMDB_ID, MOVIE))
                    .thenThrow(new CatalogUnavailableException("down", new RuntimeException()));

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Unknown Title");
        }

        @Test
        @DisplayName("Validation — invalid status filter throws 400 BAD_REQUEST")
        void getWatchlist_invalidStatusFilter_throws400() {
            assertThatThrownBy(() -> service.getWatchlist(USER_ID, "MAYBE_LATER", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — invalid type filter throws 400 BAD_REQUEST")
        void getWatchlist_invalidTypeFilter_throws400() {
            assertThatThrownBy(() -> service.getWatchlist(USER_ID, null, "PODCAST"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Isolation — returns only entries belonging to the requesting user")
        void getWatchlist_returnsOnlyCurrentUserEntries() {
            // Repository is scoped by userId — verify correct userId passed
            when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Collections.emptyList());

            service.getWatchlist(USER_ID, null, null);

            verify(repository).findByUserIdOrderByCreatedAtDesc(USER_ID);
        }
    }

    // =========================================================================
    // FR-05: Get Single Entry
    // =========================================================================

    @Nested
    @DisplayName("FR-05: getEntry()")
    class GetEntryTests {

        @Test
        @DisplayName("Happy path — returns enriched entry for valid user and tmdbId")
        void getEntry_existingEntry_returnsEnrichedResponse() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WATCHING");
            CatalogContentDto catalog = buildCatalogDto(MOVIE);

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));

            WatchlistEntryResponse result = service.getEntry(USER_ID, TMDB_ID);

            assertThat(result.getTmdbId()).isEqualTo(TMDB_ID);
            assertThat(result.getStatus()).isEqualTo("WATCHING");
            assertThat(result.getTitle()).isEqualTo("Fight Club");
        }

        @Test
        @DisplayName("Not found — 404 thrown when entry does not exist")
        void getEntry_notFound_throws404() {
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getEntry(USER_ID, TMDB_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Validation — null tmdbId throws 400 BAD_REQUEST")
        void getEntry_nullTmdbId_throws400() {
            assertThatThrownBy(() -> service.getEntry(USER_ID, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — non-positive tmdbId throws 400 BAD_REQUEST")
        void getEntry_nonPositiveTmdbId_throws400() {
            assertThatThrownBy(() -> service.getEntry(USER_ID, -5))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Catalog degraded — returns partial response with 'Unknown Title' on catalog failure")
        void getEntry_catalogUnavailable_returnsPartialResponse() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WATCHING");

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(catalogClient.findById(TMDB_ID, MOVIE))
                    .thenThrow(new RuntimeException("network error"));

            WatchlistEntryResponse result = service.getEntry(USER_ID, TMDB_ID);

            assertThat(result.getTmdbId()).isEqualTo(TMDB_ID);
            assertThat(result.getTitle()).isEqualTo("Unknown Title");
        }
    }

    // =========================================================================
    // FR-06: Public Watchlist
    // =========================================================================

    @Nested
    @DisplayName("FR-06: getPublicWatchlist()")
    class GetPublicWatchlistTests {

        @Test
        @DisplayName("Happy path — public watchlist returns entries without auth check")
        void getPublicWatchlist_validUserId_returnsEntries() {
            Long targetUserId = 42L;
            List<WatchlistEntry> entries = List.of(
                    buildEntry(targetUserId, TMDB_ID, MOVIE, "COMPLETED")
            );
            when(repository.findByUserIdOrderByCreatedAtDesc(targetUserId)).thenReturn(entries);
            when(catalogClient.findById(anyInt(), anyString())).thenReturn(Optional.empty());

            List<WatchlistEntryResponse> result = service.getPublicWatchlist(targetUserId, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Filter by status — public watchlist supports status filtering")
        void getPublicWatchlist_filterByStatus_returnsFilteredEntries() {
            Long targetUserId = 42L;
            List<WatchlistEntry> entries = List.of(
                    buildEntry(targetUserId, TMDB_ID, MOVIE, "COMPLETED")
            );
            when(repository.findByUserIdAndStatusOrderByCreatedAtDesc(targetUserId, "COMPLETED"))
                    .thenReturn(entries);
            when(catalogClient.findById(anyInt(), anyString())).thenReturn(Optional.empty());

            List<WatchlistEntryResponse> result = service.getPublicWatchlist(targetUserId, "COMPLETED", null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Empty result — returns empty list for user with no watchlist entries")
        void getPublicWatchlist_noEntries_returnsEmptyList() {
            Long targetUserId = 42L;
            when(repository.findByUserIdOrderByCreatedAtDesc(targetUserId)).thenReturn(Collections.emptyList());

            List<WatchlistEntryResponse> result = service.getPublicWatchlist(targetUserId, null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Validation — invalid status filter throws 400 BAD_REQUEST")
        void getPublicWatchlist_invalidStatus_throws400() {
            assertThatThrownBy(() -> service.getPublicWatchlist(42L, "BINGED", null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // =========================================================================
    // FR-07: Watchlist Stats
    // =========================================================================

    @Nested
    @DisplayName("FR-07: getStats()")
    class GetStatsTests {

        @Test
        @DisplayName("Happy path — stats correctly aggregated from repository counts")
        void getStats_withEntries_returnsCorrectCounts() {
            List<StatusCount> statusCounts = List.of(
                    mockStatusCount("WANT_TO_WATCH", 3L),
                    mockStatusCount("WATCHING",      2L),
                    mockStatusCount("COMPLETED",     5L)
            );
            when(repository.countByStatusForUser(USER_ID)).thenReturn(statusCounts);
            when(repository.countByUserIdAndContentType(USER_ID, "MOVIE")).thenReturn(6L);
            when(repository.countByUserIdAndContentType(USER_ID, "SERIES")).thenReturn(4L);

            WatchlistStatsResponse result = service.getStats(USER_ID);

            assertThat(result.getTotal()).isEqualTo(10L);
            assertThat(result.getWantToWatch()).isEqualTo(3L);
            assertThat(result.getWatching()).isEqualTo(2L);
            assertThat(result.getCompleted()).isEqualTo(5L);
            assertThat(result.getTotalMovies()).isEqualTo(6L);
            assertThat(result.getTotalSeries()).isEqualTo(4L);
        }

        @Test
        @DisplayName("Total calculation — total equals sum of all status counts")
        void getStats_total_equalsSumOfStatuses() {
            List<StatusCount> statusCounts = List.of(
                    mockStatusCount("WANT_TO_WATCH", 1L),
                    mockStatusCount("WATCHING",      2L),
                    mockStatusCount("COMPLETED",     3L)
            );
            when(repository.countByStatusForUser(USER_ID)).thenReturn(statusCounts);
            when(repository.countByUserIdAndContentType(USER_ID, "MOVIE")).thenReturn(3L);
            when(repository.countByUserIdAndContentType(USER_ID, "SERIES")).thenReturn(3L);

            WatchlistStatsResponse result = service.getStats(USER_ID);

            assertThat(result.getTotal()).isEqualTo(
                    result.getWantToWatch() + result.getWatching() + result.getCompleted()
            );
        }

        @Test
        @DisplayName("Empty watchlist — all stats return zero")
        void getStats_emptyWatchlist_allZero() {
            when(repository.countByStatusForUser(USER_ID)).thenReturn(Collections.emptyList());
            when(repository.countByUserIdAndContentType(USER_ID, "MOVIE")).thenReturn(0L);
            when(repository.countByUserIdAndContentType(USER_ID, "SERIES")).thenReturn(0L);

            WatchlistStatsResponse result = service.getStats(USER_ID);

            assertThat(result.getTotal()).isZero();
            assertThat(result.getWantToWatch()).isZero();
            assertThat(result.getWatching()).isZero();
            assertThat(result.getCompleted()).isZero();
            assertThat(result.getTotalMovies()).isZero();
            assertThat(result.getTotalSeries()).isZero();
        }

        @Test
        @DisplayName("Partial statuses — missing status categories default to 0 (e.g. no COMPLETED entries)")
        void getStats_missingStatusCategory_treatedAsZero() {
            // Only WANT_TO_WATCH returned — WATCHING and COMPLETED absent
            List<StatusCount> statusCounts = List.of(
                    mockStatusCount("WANT_TO_WATCH", 4L)
            );
            when(repository.countByStatusForUser(USER_ID)).thenReturn(statusCounts);
            when(repository.countByUserIdAndContentType(USER_ID, "MOVIE")).thenReturn(4L);
            when(repository.countByUserIdAndContentType(USER_ID, "SERIES")).thenReturn(0L);

            WatchlistStatsResponse result = service.getStats(USER_ID);

            assertThat(result.getWantToWatch()).isEqualTo(4L);
            assertThat(result.getWatching()).isZero();
            assertThat(result.getCompleted()).isZero();
            assertThat(result.getTotal()).isEqualTo(4L);
        }

        @Test
        @DisplayName("NFR-08: No entries loaded into memory — countByStatusForUser uses aggregate query")
        void getStats_usesAggregateQuery_notLoadAllEntries() {
            when(repository.countByStatusForUser(USER_ID)).thenReturn(Collections.emptyList());
            when(repository.countByUserIdAndContentType(anyLong(), anyString())).thenReturn(0L);

            service.getStats(USER_ID);

            // Should NEVER call findAll-style methods — only aggregate counts
            verify(repository, never()).findByUserIdOrderByCreatedAtDesc(anyLong());
            verify(repository, never()).findAll();
            verify(repository, times(1)).countByStatusForUser(USER_ID);
        }
    }

    // =========================================================================
    // FR-08: Contains Check
    // =========================================================================

    @Nested
    @DisplayName("FR-08: contains()")
    class ContainsTests {

        @Test
        @DisplayName("Happy path — returns exists=true with current status when entry found")
        void contains_entryExists_returnsTrueWithStatus() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WATCHING");
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));

            ContainsResponse result = service.contains(USER_ID, TMDB_ID);

            assertThat(result.isExists()).isTrue();
            assertThat(result.getStatus()).isEqualTo("WATCHING");
        }

        @Test
        @DisplayName("Not in watchlist — returns exists=false with null status")
        void contains_entryAbsent_returnsFalseWithNullStatus() {
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

            ContainsResponse result = service.contains(USER_ID, TMDB_ID);

            assertThat(result.isExists()).isFalse();
            assertThat(result.getStatus()).isNull();
        }

        @Test
        @DisplayName("Status reflects latest — contains returns current status after update")
        void contains_afterStatusUpdate_returnsUpdatedStatus() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "COMPLETED");
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));

            ContainsResponse result = service.contains(USER_ID, TMDB_ID);

            assertThat(result.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Validation — null tmdbId throws 400 BAD_REQUEST")
        void contains_nullTmdbId_throws400() {
            assertThatThrownBy(() -> service.contains(USER_ID, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Validation — non-positive tmdbId throws 400 BAD_REQUEST")
        void contains_nonPositiveTmdbId_throws400() {
            assertThatThrownBy(() -> service.contains(USER_ID, 0))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Isolation — contains check is scoped to the requesting user only")
        void contains_checkedAgainstCorrectUser() {
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.empty());

            service.contains(USER_ID, TMDB_ID);

            verify(repository).findByUserIdAndTmdbId(USER_ID, TMDB_ID);
        }
    }

    // =========================================================================
    // Cross-cutting: toResponse() / enrichment
    // =========================================================================

    @Nested
    @DisplayName("Response enrichment (toResponse)")
    class EnrichmentTests {

        @Test
        @DisplayName("Null catalog content — title falls back to 'Unknown Title'")
        void toResponse_nullCatalog_titleFallback() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");
            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.empty());

            WatchlistEntryResponse result = service.getEntry(USER_ID, TMDB_ID);

            assertThat(result.getTitle()).isEqualTo("Unknown Title");
            assertThat(result.getPosterUrl()).isNull();
            assertThat(result.getOverview()).isNull();
        }

        @Test
        @DisplayName("Full catalog content — all metadata fields populated in response")
        void toResponse_fullCatalog_allFieldsPopulated() {
            WatchlistEntry entry = buildEntry(USER_ID, TMDB_ID, MOVIE, "COMPLETED");
            CatalogContentDto catalog = buildCatalogDto(MOVIE);

            when(repository.findByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(Optional.of(entry));
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));

            WatchlistEntryResponse result = service.getEntry(USER_ID, TMDB_ID);

            assertThat(result.getTitle()).isEqualTo("Fight Club");
            assertThat(result.getReleaseYear()).isEqualTo(1999);
            assertThat(result.getGenre()).isEqualTo("Drama");
            assertThat(result.getOverview()).isNotBlank();
            assertThat(result.getPosterUrl()).isNotBlank();
        }
    }

    // =========================================================================
    // REGRESSION: Bug #1 — getWatchlist() reachable & returns data for valid user
    // Catches: watchlist route/service not wired, returns null or throws instead
    //          of an empty/populated list (would cause navbar link to be useless)
    // =========================================================================

    @Nested
    @DisplayName("REGRESSION Bug #1: Watchlist accessible for authenticated user")
    class WatchlistAccessRegressionTests {

        @Test
        @DisplayName("getWatchlist() returns a non-null list for a valid authenticated user")
        void getWatchlist_authenticatedUser_returnsNonNullList() {
            when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Collections.emptyList());

            List<WatchlistEntryResponse> result = service.getWatchlist(USER_ID, null, null);

            // Must never return null — an empty watchlist is a valid state
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("getWatchlist() does not throw for a user with an empty watchlist")
        void getWatchlist_emptyWatchlist_doesNotThrow() {
            when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Collections.emptyList());

            // If the watchlist route is broken this will throw instead of returning []
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> service.getWatchlist(USER_ID, null, null)
            );
        }

        @Test
        @DisplayName("getStats() returns a non-null response for a valid authenticated user")
        void getStats_authenticatedUser_returnsNonNullResponse() {
            when(repository.countByStatusForUser(USER_ID)).thenReturn(Collections.emptyList());
            when(repository.countByUserIdAndContentType(USER_ID, "MOVIE")).thenReturn(0L);
            when(repository.countByUserIdAndContentType(USER_ID, "SERIES")).thenReturn(0L);

            WatchlistStatsResponse result = service.getStats(USER_ID);

            // Stats must always be returned — needed by the navbar/profile widget
            assertThat(result).isNotNull();
        }
    }

    // =========================================================================
    // REGRESSION: Bug #2 — add() succeeds for a legitimate new entry
    // Catches: "Could not add to watchlist" shown in UI — service throwing
    //          unexpectedly (wrong validation, catalog call failing silently, etc.)
    // =========================================================================

    @Nested
    @DisplayName("REGRESSION Bug #2: Add to watchlist succeeds for valid input")
    class AddToWatchlistRegressionTests {

        @Test
        @DisplayName("add() does not throw for a valid new MOVIE entry with catalog available")
        void add_validMovieEntry_doesNotThrow() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> service.add(USER_ID, req)
            );
        }

        @Test
        @DisplayName("add() returns HTTP 201-level response (non-null) — not an error object")
        void add_validEntry_returnsPopulatedResponse() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            WatchlistEntryResponse result = service.add(USER_ID, req);

            // If this is null the frontend will always show "Could not add to watchlist"
            assertThat(result).isNotNull();
            assertThat(result.getTmdbId()).isEqualTo(TMDB_ID);
            assertThat(result.getStatus()).isEqualTo("WANT_TO_WATCH");
        }

        @Test
        @DisplayName("add() correctly calls existsByUserIdAndTmdbId before attempting save")
        void add_duplicateCheckCalledBeforeSave() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            service.add(USER_ID, req);

            // If the duplicate check is broken/missing, a DB exception becomes a 500
            // which the frontend shows as "Could not add to watchlist"
            verify(repository, times(1)).existsByUserIdAndTmdbId(USER_ID, TMDB_ID);
        }

        @Test
        @DisplayName("add() calls catalogClient.findById with correct tmdbId and contentType")
        void add_catalogCalledWithCorrectArguments() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenReturn(saved);

            service.add(USER_ID, req);

            // Wrong args here (e.g. passing "movie" instead of "MOVIE", or swapped args)
            // causes catalog to return empty → 404 → "Could not add to watchlist" in UI
            verify(catalogClient, times(1)).findById(TMDB_ID, MOVIE);
        }

        @Test
        @DisplayName("add() persists the entry with the userId from the JWT, not from the request body")
        void add_entryPersistedWithCorrectUserId() {
            AddToWatchlistRequest req = buildAddRequest(TMDB_ID, MOVIE);
            CatalogContentDto catalog = buildCatalogDto(MOVIE);
            WatchlistEntry saved = buildEntry(USER_ID, TMDB_ID, MOVIE, "WANT_TO_WATCH");

            when(repository.existsByUserIdAndTmdbId(USER_ID, TMDB_ID)).thenReturn(false);
            when(catalogClient.findById(TMDB_ID, MOVIE)).thenReturn(Optional.of(catalog));
            when(repository.save(any(WatchlistEntry.class))).thenAnswer(invocation -> {
                WatchlistEntry e = invocation.getArgument(0);
                // userId must be set from the authenticated principal, not be null
                assertThat(e.getUserId()).isEqualTo(USER_ID);
                assertThat(e.getTmdbId()).isEqualTo(TMDB_ID);
                assertThat(e.getContentType()).isEqualTo(MOVIE);
                assertThat(e.getStatus()).isEqualTo("WANT_TO_WATCH");
                assertThat(e.getCreatedAt()).isNotNull();
                assertThat(e.getUpdatedAt()).isNotNull();
                return saved;
            });

            service.add(USER_ID, req);
        }
    }

    // =========================================================================
    // Helper: mock StatusCount projection
    // =========================================================================

    private StatusCount mockStatusCount(String status, long count) {
        return new StatusCount() {
            @Override public String getStatus() { return status; }
            @Override public Long   getCount()  { return count;  }
        };
    }

}

// =============================================================================
// REGRESSION: "Failed to resolve user" — 500 on every watchlist request
//
// The logs show every /api/watchlist/** call returning:
//   500 INTERNAL_SERVER_ERROR "Failed to resolve user"
// This means UserResolverImpl.resolveUserIdByEmail() throws a SQLException on
// every request — wrong table name, wrong column name, or broken DataSource.
// These tests run against a real embedded H2 DB to catch exactly that.
// Mocks cannot catch SQL bugs — this must use a real DB.
//
// Kept in the same file as WatchlistServiceTest but as a separate top-level
// class (required because @JdbcTest and @ExtendWith(MockitoExtension) cannot
// share the same class-level annotations).
// =============================================================================

@org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
@org.springframework.context.annotation.Import(
        com.bingeboxed.watchlist.controller.UserResolverImpl.class)
@org.springframework.test.context.ActiveProfiles("test")
@DisplayName("UserResolverImpl — REGRESSION: 500 Failed to resolve user")
class UserResolverImplTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    private com.bingeboxed.watchlist.controller.UserResolverImpl userResolver;

    // Schema matches the real production `
    // ` table exactly:
    // columns: id, created_at, email, password
    @org.junit.jupiter.api.BeforeEach
    void createUsersTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_users");
        jdbcTemplate.execute(
                "CREATE TABLE auth_users (" +
                        "    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY," +
                        "    created_at TIMESTAMP," +
                        "    email      VARCHAR(255) NOT NULL UNIQUE," +
                        "    password   VARCHAR(255) NOT NULL" +
                        ")"
        );
    }

    // -------------------------------------------------------------------------
    // CRITICAL: This is the test that catches the actual bug.
    //
    // Every request returns 500 "Failed to resolve user". The catch block in
    // UserResolverImpl swallows ALL SQLExceptions and converts them to 500.
    // This means any SQLException — including ones caused by a bad DataSource
    // config — is invisible. This test exposes the raw exception by asserting
    // the response is NOT a 500, forcing the real cause to surface if broken.
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CRITICAL — resolveUserIdByEmail does NOT throw 500 for a registered user")
    void resolveUserIdByEmail_registeredUser_doesNotReturn500() {
        jdbcTemplate.update(
                "INSERT INTO auth_users (email, password) VALUES (?, ?)",
                "ssher9@uic.edu", "$2a$10$hashedpassword"
        );

        // This is what happens on every watchlist request — if this throws
        // ResponseStatusException with status 500, the bug is still present.
        // The fix must result in a valid Long being returned here.
        assertThatThrownBy(() -> userResolver.resolveUserIdByEmail("ssher9@uic.edu"))
                .isNotInstanceOf(org.springframework.web.server.ResponseStatusException.class);

        // Or if it doesn't throw at all — that's the correct behaviour
        Long userId = userResolver.resolveUserIdByEmail("ssher9@uic.edu");
        assertThat(userId).isNotNull().isPositive();
    }

    @Test
    @DisplayName("CRITICAL — 500 is never returned for any valid email lookup")
    void resolveUserIdByEmail_neverReturns500ForValidEmail() {
        jdbcTemplate.update(
                "INSERT INTO auth_users (email, password) VALUES (?, ?)",
                "test@bingeboxed.com", "$2a$10$hashedpassword"
        );

        try {
            userResolver.resolveUserIdByEmail("test@bingeboxed.com");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // 401 is acceptable (user not found), 500 is the bug
            assertThat(e.getStatusCode())
                    .as("Expected 401 at most, but got 500 — SQLException is being swallowed")
                    .isNotEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Test
    @DisplayName("Returns correct userId for a user matching the real schema")
    void resolveUserIdByEmail_realSchema_returnsCorrectId() {
        jdbcTemplate.update(
                "INSERT INTO auth_users (email, password) VALUES (?, ?)",
                "user@bingeboxed.com", "$2a$10$hashedpassword"
        );

        Long returned = userResolver.resolveUserIdByEmail("user@bingeboxed.com");
        Long actual   = jdbcTemplate.queryForObject(
                "SELECT id FROM auth_users WHERE email = ?", Long.class, "user@bingeboxed.com");

        assertThat(returned).isEqualTo(actual);
    }

    @Test
    @DisplayName("Unknown email — throws 401 UNAUTHORIZED, not 500")
    void resolveUserIdByEmail_unknownEmail_throws401() {
        assertThatThrownBy(() -> userResolver.resolveUserIdByEmail("ghost@example.com"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e -> assertThat(
                        ((org.springframework.web.server.ResponseStatusException) e).getStatusCode())
                        .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Null email — throws 401 without hitting the DB")
    void resolveUserIdByEmail_nullEmail_throws401() {
        assertThatThrownBy(() -> userResolver.resolveUserIdByEmail(null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e -> assertThat(
                        ((org.springframework.web.server.ResponseStatusException) e).getStatusCode())
                        .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Blank email — throws 401 without hitting the DB")
    void resolveUserIdByEmail_blankEmail_throws401() {
        assertThatThrownBy(() -> userResolver.resolveUserIdByEmail("   "))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e -> assertThat(
                        ((org.springframework.web.server.ResponseStatusException) e).getStatusCode())
                        .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED));
    }
}