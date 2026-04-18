// src/main/java/com/bingeboxed/auth/service/AuthService.java
package com.bingeboxed.auth.service;

import com.bingeboxed.auth.dto.AuthResponse;
import com.bingeboxed.auth.dto.LoginRequest;
import com.bingeboxed.auth.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}