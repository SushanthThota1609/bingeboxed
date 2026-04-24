// src/main/java/com/bingeboxed/shared/user/CurrentUserService.java
package com.bingeboxed.shared.user;

import com.bingeboxed.auth.entity.User;
import com.bingeboxed.auth.repository.UserRepository;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import com.bingeboxed.shared.security.JwtService;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    
    private final UserRepository userRepository;
    private final JwtService jwtService;
    
    public CurrentUserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }
    
    public Long getCurrentUserId(String token) {
        String email = jwtService.extractEmail(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid token");
        }
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
    
    public String getUserDisplayName(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getEmail();
    }
}