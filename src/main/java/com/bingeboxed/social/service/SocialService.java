package com.bingeboxed.social.service;

import com.bingeboxed.social.dto.*;

import java.util.List;

public interface SocialService {
    List<UserSearchResponse> searchUsers(String query, Long currentUserId);
    PublicProfileResponse getUserProfile(Long userId, Long currentUserId);
    FriendRequestDto sendFriendRequest(Long senderId, Long receiverId);
    List<FriendRequestDto> getIncomingRequests(Long userId);
    List<FriendRequestDto> getOutgoingRequests(Long userId);
    void acceptFriendRequest(Long requestId, Long currentUserId);
    void declineFriendRequest(Long requestId, Long currentUserId);
    List<FriendDto> getFriends(Long userId);
    SocialCountsResponse getSocialCounts(Long userId);
}
