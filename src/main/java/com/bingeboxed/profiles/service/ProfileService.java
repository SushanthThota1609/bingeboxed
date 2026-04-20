package com.bingeboxed.profiles.service;

import com.bingeboxed.auth.entity.User;
import com.bingeboxed.auth.repository.UserRepository;
import com.bingeboxed.profiles.dto.PublicProfileResponse;
import com.bingeboxed.profiles.dto.UpdateProfileRequest;
import com.bingeboxed.profiles.entity.Profile;
import com.bingeboxed.profiles.exception.ProfileNotFoundException;
import com.bingeboxed.profiles.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Profile getOrCreateProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile newProfile = new Profile(userId);
                    return profileRepository.save(newProfile);
                });
    }

    @Transactional(readOnly = true)
    public Profile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user id: " + userId));
    }

    @Transactional
    public Profile updateProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = getOrCreateProfile(userId);

        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        profile.setUpdatedAt(LocalDateTime.now());

        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long userId) {
        Profile profile = getProfileByUserId(userId);
        return new PublicProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ProfileNotFoundException("User not found with email: " + email));
    }
}