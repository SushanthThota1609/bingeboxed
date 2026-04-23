// src/main/java/com/bingeboxed/watchlist/dto/WatchlistStatsResponse.java
package com.bingeboxed.watchlist.dto;

public class WatchlistStatsResponse {

    private long total;
    private long wantToWatch;
    private long watching;
    private long completed;
    private long totalMovies;
    private long totalSeries;

    public WatchlistStatsResponse() {}

    public long getTotal()        { return total; }
    public void setTotal(long v)  { this.total = v; }

    public long getWantToWatch()        { return wantToWatch; }
    public void setWantToWatch(long v)  { this.wantToWatch = v; }

    public long getWatching()           { return watching; }
    public void setWatching(long v)     { this.watching = v; }

    public long getCompleted()          { return completed; }
    public void setCompleted(long v)    { this.completed = v; }

    public long getTotalMovies()        { return totalMovies; }
    public void setTotalMovies(long v)  { this.totalMovies = v; }

    public long getTotalSeries()        { return totalSeries; }
    public void setTotalSeries(long v)  { this.totalSeries = v; }
}