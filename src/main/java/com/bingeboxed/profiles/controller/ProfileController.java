package com.bingeboxed.profiles.controller;

import com.bingeboxed.auth.entity.User;
import com.bingeboxed.profiles.dto.ProfileResponse;
import com.bingeboxed.profiles.dto.PublicProfileResponse;
import com.bingeboxed.profiles.dto.UpdateProfileRequest;
import com.bingeboxed.profiles.entity.Profile;
import com.bingeboxed.profiles.service.ProfileService;
import com.bingeboxed.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;

    public ProfileController(ProfileService profileService, JwtService jwtService) {
        this.profileService = profileService;
        this.jwtService = jwtService;
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }
        String email = jwtService.extractEmail(token);
        User user = profileService.getUserByEmail(email);
        return user.getId();
    }

    @GetMapping("/api/profiles/me")
    public ResponseEntity<ProfileResponse> getMyProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Profile profile = profileService.getOrCreateProfile(userId);
        String email = profileService.getUserByEmail(jwtService.extractEmail(extractTokenFromRequest(request))).getEmail();
        return ResponseEntity.ok(new ProfileResponse(profile, email));
    }

    @GetMapping("/api/profiles/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long id, HttpServletRequest request) {
        Profile profile = profileService.getProfileByUserId(id);
        String email = profileService.getUserByEmail(jwtService.extractEmail(extractTokenFromRequest(request))).getEmail();
        return ResponseEntity.ok(new ProfileResponse(profile, email));
    }

    @PutMapping("/api/profiles/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(@RequestBody UpdateProfileRequest request, HttpServletRequest req) {
        Long userId = getCurrentUserId(req);
        Profile updated = profileService.updateProfile(userId, request);
        String email = profileService.getUserByEmail(jwtService.extractEmail(extractTokenFromRequest(req))).getEmail();
        return ResponseEntity.ok(new ProfileResponse(updated, email));
    }

    @GetMapping("/api/profiles/public/{id}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable Long id) {
        PublicProfileResponse publicProfile = profileService.getPublicProfile(id);
        return ResponseEntity.ok(publicProfile);
    }

    @GetMapping("/profile")
    public ModelAndView viewProfilePage(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("profiles/view");
        try {
            Long userId = getCurrentUserId(request);
            if (userId != null) {
                Profile profile = profileService.getOrCreateProfile(userId);
                User user = profileService.getUserByEmail(jwtService.extractEmail(extractTokenFromRequest(request)));
                mav.addObject("profile", new ProfileResponse(profile, user.getEmail()));
            }
        } catch (Exception e) {
            // If token is invalid or missing, just render view without model data
            // The frontend JavaScript will handle redirect to login
        }
        return mav;
    }

    @GetMapping("/profile/edit")
    public ModelAndView editProfilePage(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("profiles/edit");
        try {
            Long userId = getCurrentUserId(request);
            if (userId != null) {
                Profile profile = profileService.getOrCreateProfile(userId);
                User user = profileService.getUserByEmail(jwtService.extractEmail(extractTokenFromRequest(request)));
                mav.addObject("profile", new ProfileResponse(profile, user.getEmail()));
            }
        } catch (Exception e) {
            // If token is invalid or missing, just render view without model data
        }
        return mav;
    }
}