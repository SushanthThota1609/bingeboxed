package com.bingeboxed.catalog.dto;

import java.util.List;

public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int totalPages;
    private int totalResults;

    public PagedResponse() {}

    public PagedResponse(List<T> content, int page, int totalPages, int totalResults) {
        this.content = content;
        this.page = page;
        this.totalPages = totalPages;
        this.totalResults = totalResults;
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
}