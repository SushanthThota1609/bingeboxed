package com.bingeboxed.profiles.dto;

import com.bingeboxed.profiles.entity.Profile;

public class PublicProfileResponse {
    private String displayName;
    private String bio;
    private String avatarUrl;

    public PublicProfileResponse() {}

    public PublicProfileResponse(Profile profile) {
        this.displayName = profile.getDisplayName();
        this.bio = profile.getBio();
        this.avatarUrl = profile.getAvatarUrl();
    }

    // Getters and setters
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}