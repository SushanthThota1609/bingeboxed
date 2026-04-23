package com.bingeboxed.social.dto;

public class SocialCountsResponse {
    private long friendsCount;
    private long pendingRequestsCount;

    public SocialCountsResponse() {}

    public SocialCountsResponse(long friendsCount, long pendingRequestsCount) {
        this.friendsCount = friendsCount;
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public long getFriendsCount() { return friendsCount; }
    public void setFriendsCount(long friendsCount) { this.friendsCount = friendsCount; }

    public long getPendingRequestsCount() { return pendingRequestsCount; }
    public void setPendingRequestsCount(long pendingRequestsCount) { this.pendingRequestsCount = pendingRequestsCount; }
}
