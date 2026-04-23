package com.bingeboxed.catalog.dto;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> content;
    private int page;
    private int totalPages;
    private long totalResults;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> content, int page, int totalPages, long totalResults) {
        this.content = content;
        this.page = page;
        this.totalPages = totalPages;
        this.totalResults = totalResults;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }
}