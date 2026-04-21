package com.bingeboxed.catalog.dto;

import java.util.List;

public class PagedResponseDto<T> extends PagedResponse<T> {

    public PagedResponseDto() {
        super();
    }

    public PagedResponseDto(List<T> content, int page, int totalPages, int totalResults) {
        super(content, page, totalPages, totalResults);
    }
}