package com.bingeboxed.catalog.service;

import com.bingeboxed.catalog.dto.ContentResponse;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PaginatedResponse;
import com.bingeboxed.catalog.dto.TrendingItemDto;

import java.util.List;
import java.util.Optional;

public interface CatalogService {
    ContentResponse getContentByTmdbId(Integer tmdbId, String type, boolean forceRefresh);
    PaginatedResponse<ContentResponse> searchContent(String query, String type, int page);
    PaginatedResponse<ContentResponse> browseContent(String type, String genre, Integer year, int page);
    ContentResponse getFullDetail(Integer tmdbId, String type);
    List<GenreDto> getGenres(String type);
    List<TrendingItemDto> getTrending(String type, String window);
}