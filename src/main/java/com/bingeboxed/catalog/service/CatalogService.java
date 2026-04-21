package com.bingeboxed.catalog.service;

import com.bingeboxed.catalog.dto.CatalogResult;
import com.bingeboxed.catalog.dto.ContentDto;
import com.bingeboxed.catalog.dto.GenreDto;
import com.bingeboxed.catalog.dto.PagedResponseDto;

import java.util.List;

public interface CatalogService {

    CatalogResult<ContentDto> fetchContent(int tmdbId, String type);

    PagedResponseDto<ContentDto> searchContent(String query, String type, int page);

    PagedResponseDto<ContentDto> browseContent(String type, String genre, Integer year, int page);

    ContentDto fetchDetail(int tmdbId, String type);

    List<GenreDto> fetchGenres(String type);

    List<ContentDto> fetchTrending(String type, String window);
}