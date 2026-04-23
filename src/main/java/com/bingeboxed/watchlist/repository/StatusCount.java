// src/main/java/com/bingeboxed/watchlist/repository/dto/StatusCount.java
package com.bingeboxed.watchlist.repository.dto;

/**
 * Projection interface used by the aggregate query in WatchlistRepository.
 */
public interface StatusCount {
    String getStatus();
    Long   getCount();
}