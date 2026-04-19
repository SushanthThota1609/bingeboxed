// src/main/java/com/bingeboxed/profiles/controller/ProfileApiController.java
package com.bingeboxed.profiles.controller;

import com.bingeboxed.profiles.dto.ProfileResponse;
import com.bingeboxed.profiles.dto.PublicProfileResponse;
import com.bingeboxed.profiles.dto.UpdateProfileRequest;
import com.bingeboxed.profiles.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileApiController {

    private final ProfileService profileService;

    public ProfileApiController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(profileService.getOrCreateProfileByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.getProfileById(id));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal String email,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(email, request));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.getPublicProfileById(id));
    }
}