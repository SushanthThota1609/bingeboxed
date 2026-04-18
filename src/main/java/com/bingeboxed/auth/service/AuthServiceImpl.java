// src/main/java/com/bingeboxed/auth/service/AuthServiceImpl.java
package com.bingeboxed.auth.service;

import com.bingeboxed.auth.dto.AuthResponse;
import com.bingeboxed.auth.dto.LoginRequest;
import com.bingeboxed.auth.dto.RegisterRequest;
import com.bingeboxed.auth.entity.AuthUser;
import com.bingeboxed.auth.repository.AuthUserRepository;
import com.bingeboxed.shared.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AuthUserRepository authUserRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public void register(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        AuthUser user = new AuthUser();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        authUserRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token);
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}