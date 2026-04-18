// src/main/java/com/bingeboxed/auth/dto/AuthResponse.java
package com.bingeboxed.auth.dto;

public class AuthResponse {

    private String token;

    public AuthResponse() {}

    public AuthResponse(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}