// src/main/java/com/bingeboxed/profiles/controller/PublicProfileController.java
package com.bingeboxed.profiles.controller;

import com.bingeboxed.profiles.dto.PublicProfileResponse;
import com.bingeboxed.profiles.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Separate controller so Spring Security's permitAll() rule for
 * /api/profiles/public/** can be applied without a JWT filter match
 * against the broader /api/profiles/** pattern.
 */
@RestController
@RequestMapping("/api/profiles/public")
public class PublicProfileController {

    private final ProfileService profileService;

    public PublicProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicProfile(@PathVariable Long id) {
        try {
            PublicProfileResponse profile = profileService.getPublicProfile(id);
            return ResponseEntity.ok(profile);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", ex.getReason() != null ? ex.getReason() : "Not found"));
        }
    }
}