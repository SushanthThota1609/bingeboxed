package com.bingeboxed.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentResponse {
    private Integer tmdbId;
    private String title;
    private String type;
    private String overview;
    private Integer releaseYear;
    private String genre;
    private String posterUrl;

    public ContentResponse() {
    }

    public ContentResponse(Integer tmdbId, String title, String type, String overview,
                           Integer releaseYear, String genre, String posterUrl) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.type = type;
        this.overview = overview;
        this.releaseYear = releaseYear;
        this.genre = genre;
        this.posterUrl = posterUrl;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}