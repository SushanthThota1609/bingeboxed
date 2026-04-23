// src/main/java/com/bingeboxed/watchlist/dto/UpdateStatusRequest.java
package com.bingeboxed.watchlist.dto;

public class UpdateStatusRequest {

    private String status;

    public UpdateStatusRequest() {}

    public String getStatus()         { return status; }
    public void   setStatus(String v) { this.status = v; }
}