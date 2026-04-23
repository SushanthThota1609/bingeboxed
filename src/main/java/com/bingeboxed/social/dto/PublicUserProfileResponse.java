package com.bingeboxed.social.dto;

public class PublicUserProfileResponse {
    private Long userId;
    private String displayName;
    private String email;
    private String friendshipStatus;
    private long followerCount;
    private long followingCount;

    public PublicUserProfileResponse() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }

    public long getFollowerCount() { return followerCount; }
    public void setFollowerCount(long followerCount) { this.followerCount = followerCount; }

    public long getFollowingCount() { return followingCount; }
    public void setFollowingCount(long followingCount) { this.followingCount = followingCount; }
}
