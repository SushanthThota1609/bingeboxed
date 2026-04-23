package com.bingeboxed.watchlist.dto;

public class ContainsResponse {

    private boolean exists;
    private String status;

    public ContainsResponse(boolean exists, String status) {
        this.exists = exists;
        this.status = status;
    }

    public boolean isExists() { return exists; }
    public void setExists(boolean exists) { this.exists = exists; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}