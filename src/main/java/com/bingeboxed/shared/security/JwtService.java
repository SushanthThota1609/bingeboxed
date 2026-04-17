// src/main/java/com/bingeboxed/shared/security/JwtService.java
package com.bingeboxed.shared.security;

public interface JwtService {
    String generateToken(String email);
    String extractEmail(String token);
    boolean isTokenValid(String token);
}