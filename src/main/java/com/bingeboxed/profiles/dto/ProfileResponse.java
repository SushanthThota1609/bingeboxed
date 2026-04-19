// src/main/java/com/bingeboxed/profiles/dto/ProfileResponse.java
package com.bingeboxed.profiles.dto;

import java.time.LocalDateTime;

public class ProfileResponse {

    private Long userId;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private LocalDateTime updatedAt;

    public ProfileResponse() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}