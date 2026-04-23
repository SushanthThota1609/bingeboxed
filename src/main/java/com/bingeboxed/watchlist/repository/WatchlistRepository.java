// src/main/java/com/bingeboxed/watchlist/repository/WatchlistRepository.java
package com.bingeboxed.watchlist.repository;

import com.bingeboxed.watchlist.entity.WatchlistEntry;
import com.bingeboxed.watchlist.repository.dto.StatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistEntry, Long> {

    Optional<WatchlistEntry> findByUserIdAndTmdbId(Long userId, Integer tmdbId);

    List<WatchlistEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WatchlistEntry> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<WatchlistEntry> findByUserIdAndContentTypeOrderByCreatedAtDesc(Long userId, String contentType);

    List<WatchlistEntry> findByUserIdAndStatusAndContentTypeOrderByCreatedAtDesc(
            Long userId, String status, String contentType);

    boolean existsByUserIdAndTmdbId(Long userId, Integer tmdbId);

    // NFR-08: aggregate query — no records loaded into memory
    @Query("""
        SELECT e.status AS status, COUNT(e) AS count
        FROM WatchlistEntry e
        WHERE e.userId = :userId
        GROUP BY e.status
        """)
    List<StatusCount> countByStatusForUser(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(e)
        FROM WatchlistEntry e
        WHERE e.userId = :userId AND e.contentType = :contentType
        """)
    long countByUserIdAndContentType(
            @Param("userId") Long userId,
            @Param("contentType") String contentType);
}