// src/main/java/com/bingeboxed/watchlist/dto/AddToWatchlistRequest.java
package com.bingeboxed.watchlist.dto;

public class AddToWatchlistRequest {

    private Integer tmdbId;
    private String  contentType;

    public AddToWatchlistRequest() {}

    public Integer getTmdbId()           { return tmdbId; }
    public void    setTmdbId(Integer v)  { this.tmdbId = v; }

    public String  getContentType()      { return contentType; }
    public void    setContentType(String v) { this.contentType = v; }
}