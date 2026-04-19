// src/main/java/com/bingeboxed/profiles/service/ProfileService.java
package com.bingeboxed.profiles.service;

import com.bingeboxed.profiles.dto.ProfileResponse;
import com.bingeboxed.profiles.dto.PublicProfileResponse;
import com.bingeboxed.profiles.dto.UpdateProfileRequest;
import com.bingeboxed.profiles.entity.UserProfile;
import com.bingeboxed.profiles.repository.UserProfileRepository;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final AuthUserLookupService authUserLookupService;

    public ProfileService(UserProfileRepository profileRepository,
                          AuthUserLookupService authUserLookupService) {
        this.profileRepository = profileRepository;
        this.authUserLookupService = authUserLookupService;
    }

    @Transactional
    public ProfileResponse getOrCreateProfileByEmail(String email) {
        Long userId = authUserLookupService.getUserIdByEmail(email);
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));
        return toProfileResponse(profile, email);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));
        String email = authUserLookupService.getEmailByUserId(userId);
        return toProfileResponse(profile, email);
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        Long userId = authUserLookupService.getUserIdByEmail(email);
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));

        if (request.getDisplayName() != null) {
            profile.setDisplayName(sanitize(request.getDisplayName(), 100));
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(sanitize(request.getAvatarUrl(), 500));
        }

        UserProfile saved = profileRepository.save(profile);
        return toProfileResponse(saved, email);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfileById(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));
        return toPublicProfileResponse(profile);
    }

    private UserProfile createEmptyProfile(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        return profileRepository.save(profile);
    }

    private ProfileResponse toProfileResponse(UserProfile profile, String email) {
        ProfileResponse response = new ProfileResponse();
        response.setUserId(profile.getUserId());
        response.setEmail(email);
        response.setDisplayName(profile.getDisplayName());
        response.setBio(profile.getBio());
        response.setAvatarUrl(profile.getAvatarUrl());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }

    private PublicProfileResponse toPublicProfileResponse(UserProfile profile) {
        PublicProfileResponse response = new PublicProfileResponse();
        response.setDisplayName(profile.getDisplayName());
        response.setBio(profile.getBio());
        response.setAvatarUrl(profile.getAvatarUrl());
        return response;
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}