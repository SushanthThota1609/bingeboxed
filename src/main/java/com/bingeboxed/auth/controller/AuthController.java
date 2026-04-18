// src/main/java/com/bingeboxed/auth/controller/AuthController.java
package com.bingeboxed.auth.controller;

import com.bingeboxed.auth.dto.AuthResponse;
import com.bingeboxed.auth.dto.LoginRequest;
import com.bingeboxed.auth.dto.RegisterRequest;
import com.bingeboxed.auth.service.AuthService;
import com.bingeboxed.auth.service.AuthServiceImpl.EmailAlreadyExistsException;
import com.bingeboxed.auth.service.AuthServiceImpl.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody("Invalid email format"));
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody("Password must be at least 8 characters"));
        }

        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorBody("Email already registered"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorBody("Invalid credentials"));
        }
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }
}