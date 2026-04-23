// src/main/java/com/bingeboxed/shared/client/CatalogContentDto.java
package com.bingeboxed.shared.client;

/**
 * Mirror of com.bingeboxed.catalog.dto.ContentDto kept in shared.client
 * so no cross-package import is needed.
 */
public class CatalogContentDto {

    private Integer tmdbId;
    private String  title;
    private String  contentType;
    private String  overview;
    private Integer releaseYear;
    private String  genre;
    private String  posterUrl;

    public CatalogContentDto() {}

    public Integer getTmdbId()      { return tmdbId; }
    public void setTmdbId(Integer v){ this.tmdbId = v; }

    public String getTitle()        { return title; }
    public void setTitle(String v)  { this.title = v; }

    public String getContentType()          { return contentType; }
    public void setContentType(String v)    { this.contentType = v; }

    public String getOverview()             { return overview; }
    public void setOverview(String v)       { this.overview = v; }

    public Integer getReleaseYear()         { return releaseYear; }
    public void setReleaseYear(Integer v)   { this.releaseYear = v; }

    public String getGenre()                { return genre; }
    public void setGenre(String v)          { this.genre = v; }

    public String getPosterUrl()            { return posterUrl; }
    public void setPosterUrl(String v)      { this.posterUrl = v; }
}