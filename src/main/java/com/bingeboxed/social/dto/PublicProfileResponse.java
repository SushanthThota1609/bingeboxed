package com.bingeboxed.social.dto;

public class PublicProfileResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String friendshipStatus;
    private long followerCount;
    private long followingCount;

    public PublicProfileResponse(Long userId, String username, String displayName, String avatarUrl,
                                 String friendshipStatus, long followerCount, long followingCount) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.friendshipStatus = friendshipStatus;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }
    public long getFollowerCount() { return followerCount; }
    public void setFollowerCount(long followerCount) { this.followerCount = followerCount; }
    public long getFollowingCount() { return followingCount; }
    public void setFollowingCount(long followingCount) { this.followingCount = followingCount; }
}
