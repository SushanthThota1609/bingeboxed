package com.bingeboxed.social.dto;

public class FriendDto {
    private Long userId;
    private String displayName;
    private String email;

    public FriendDto() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
