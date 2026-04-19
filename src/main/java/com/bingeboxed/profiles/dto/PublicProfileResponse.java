// src/main/java/com/bingeboxed/profiles/dto/PublicProfileResponse.java
package com.bingeboxed.profiles.dto;

public class PublicProfileResponse {

    private String displayName;
    private String bio;
    private String avatarUrl;

    public PublicProfileResponse() {}

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}