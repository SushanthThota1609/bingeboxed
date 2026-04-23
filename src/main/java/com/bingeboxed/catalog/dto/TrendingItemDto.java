package com.bingeboxed.catalog.dto;

public class TrendingItemDto {
    private Integer tmdbId;
    private String title;
    private String type;
    private String posterUrl;
    private Integer releaseYear;
    private String genre;

    public TrendingItemDto() {
    }

    public TrendingItemDto(Integer tmdbId, String title, String type, String posterUrl,
                           Integer releaseYear, String genre) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.type = type;
        this.posterUrl = posterUrl;
        this.releaseYear = releaseYear;
        this.genre = genre;
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

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
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
}