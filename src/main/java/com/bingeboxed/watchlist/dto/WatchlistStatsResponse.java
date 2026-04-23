package com.bingeboxed.watchlist.dto;

public class WatchlistStatsResponse {

    private long total;
    private long wantToWatch;
    private long watching;
    private long completed;
    private long totalMovies;
    private long totalSeries;

    public WatchlistStatsResponse() {}

    public WatchlistStatsResponse(long total, long wantToWatch, long watching, long completed,
                                  long totalMovies, long totalSeries) {
        this.total = total;
        this.wantToWatch = wantToWatch;
        this.watching = watching;
        this.completed = completed;
        this.totalMovies = totalMovies;
        this.totalSeries = totalSeries;
    }

    // Getters and setters
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getWantToWatch() { return wantToWatch; }
    public void setWantToWatch(long wantToWatch) { this.wantToWatch = wantToWatch; }

    public long getWatching() { return watching; }
    public void setWatching(long watching) { this.watching = watching; }

    public long getCompleted() { return completed; }
    public void setCompleted(long completed) { this.completed = completed; }

    public long getTotalMovies() { return totalMovies; }
    public void setTotalMovies(long totalMovies) { this.totalMovies = totalMovies; }

    public long getTotalSeries() { return totalSeries; }
    public void setTotalSeries(long totalSeries) { this.totalSeries = totalSeries; }
}