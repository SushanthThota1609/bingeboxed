// src/main/java/com/bingeboxed/profiles/service/AuthUserLookupService.java
package com.bingeboxed.profiles.service;

import com.bingeboxed.auth.repository.AuthUserRepository;
import com.bingeboxed.auth.entity.AuthUser;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthUserLookupService {

    private final AuthUserRepository authUserRepository;

    public AuthUserLookupService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        return authUserRepository.findByEmail(email)
                .map(AuthUser::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public String getEmailByUserId(Long userId) {
        return authUserRepository.findById(userId)
                .map(AuthUser::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}