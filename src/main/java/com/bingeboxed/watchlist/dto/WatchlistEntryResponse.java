// src/main/java/com/bingeboxed/watchlist/dto/WatchlistEntryResponse.java
package com.bingeboxed.watchlist.dto;

/**
 * Enriched response DTO — combines watchlist entry data with
 * content metadata from CatalogClient (NFR-07).
 */
public class WatchlistEntryResponse {

    private Integer tmdbId;
    private String  contentType;
    private String  status;
    private String  title;
    private Integer releaseYear;
    private String  genre;
    private String  posterUrl;
    private String  overview;

    public WatchlistEntryResponse() {}

    public Integer getTmdbId()              { return tmdbId; }
    public void    setTmdbId(Integer v)     { this.tmdbId = v; }

    public String  getContentType()         { return contentType; }
    public void    setContentType(String v) { this.contentType = v; }

    public String  getStatus()              { return status; }
    public void    setStatus(String v)      { this.status = v; }

    public String  getTitle()               { return title; }
    public void    setTitle(String v)       { this.title = v; }

    public Integer getReleaseYear()         { return releaseYear; }
    public void    setReleaseYear(Integer v){ this.releaseYear = v; }

    public String  getGenre()               { return genre; }
    public void    setGenre(String v)       { this.genre = v; }

    public String  getPosterUrl()           { return posterUrl; }
    public void    setPosterUrl(String v)   { this.posterUrl = v; }

    public String  getOverview()            { return overview; }
    public void    setOverview(String v)    { this.overview = v; }
}