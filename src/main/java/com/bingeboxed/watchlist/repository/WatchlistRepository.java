package com.bingeboxed.watchlist.repository;

import com.bingeboxed.watchlist.entity.WatchlistEntry;
import com.bingeboxed.watchlist.entity.WatchlistEntry.ContentType;
import com.bingeboxed.watchlist.entity.WatchlistEntry.WatchlistStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistEntry, Long> {

    Optional<WatchlistEntry> findByUserIdAndTmdbId(Long userId, Integer tmdbId);

    List<WatchlistEntry> findByUserId(Long userId, Sort sort);

    List<WatchlistEntry> findByUserIdAndStatus(Long userId, WatchlistStatus status, Sort sort);

    List<WatchlistEntry> findByUserIdAndContentType(Long userId, ContentType contentType, Sort sort);

    List<WatchlistEntry> findByUserIdAndStatusAndContentType(Long userId, WatchlistStatus status, ContentType contentType, Sort sort);

    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);

    @Query("SELECT COUNT(w) FROM WatchlistEntry w WHERE w.userId = :userId")
    long countTotalByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) FROM WatchlistEntry w WHERE w.userId = :userId AND w.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") WatchlistStatus status);

    @Query("SELECT COUNT(w) FROM WatchlistEntry w WHERE w.userId = :userId AND w.contentType = :contentType")
    long countByUserIdAndContentType(@Param("userId") Long userId, @Param("contentType") ContentType contentType);
}