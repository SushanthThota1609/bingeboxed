// src/main/java/com/bingeboxed/watchlist/dto/ContainsResponse.java
package com.bingeboxed.watchlist.dto;

public class ContainsResponse {

    private boolean exists;
    private String  status;

    public ContainsResponse() {}

    public ContainsResponse(boolean exists, String status) {
        this.exists = exists;
        this.status = status;
    }

    public boolean isExists()         { return exists; }
    public void    setExists(boolean v){ this.exists = v; }

    public String  getStatus()        { return status; }
    public void    setStatus(String v){ this.status = v; }
}