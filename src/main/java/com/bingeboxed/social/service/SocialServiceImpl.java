package com.bingeboxed.social.service;

import com.bingeboxed.auth.entity.User;
import com.bingeboxed.auth.repository.UserRepository;
import com.bingeboxed.profiles.entity.Profile;
import com.bingeboxed.profiles.repository.ProfileRepository;
import com.bingeboxed.shared.client.SocialGraphClient;
import com.bingeboxed.social.dto.*;
import com.bingeboxed.social.entity.FriendRequest;
import com.bingeboxed.social.entity.FriendRequestStatus;
import com.bingeboxed.social.entity.Friendship;
import com.bingeboxed.social.repository.FriendRequestRepository;
import com.bingeboxed.social.repository.FriendshipRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SocialServiceImpl implements SocialService, SocialGraphClient {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final EntityManager entityManager;

    public SocialServiceImpl(UserRepository userRepository,
                             ProfileRepository profileRepository,
                             FriendRequestRepository friendRequestRepository,
                             FriendshipRepository friendshipRepository,
                             EntityManager entityManager) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.entityManager = entityManager;
    }

    @Override
    public List<Long> getFollowing(Long userId) {
        return friendshipRepository.findFriendIdsByUserId(userId);
    }

    @Override
    public List<Long> getFollowers(Long userId) {
        return friendshipRepository.findFriendIdsByUserId(userId);
    }

    @Override
    public List<UserSearchResponse> searchUsers(String query, Long currentUserId) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String searchPattern = "%" + query.toLowerCase() + "%";
        String jpql = "SELECT u.id, u.email, p.displayName, p.avatarUrl " +
                      "FROM User u LEFT JOIN Profile p ON u.id = p.userId " +
                      "WHERE (LOWER(u.email) LIKE :pattern OR LOWER(p.displayName) LIKE :pattern) " +
                      "AND u.id != :currentUserId";
        Query nativeQuery = entityManager.createQuery(jpql);
        nativeQuery.setParameter("pattern", searchPattern);
        nativeQuery.setParameter("currentUserId", currentUserId);
        List<Object[]> results = nativeQuery.getResultList();
        return results.stream()
                .map(row -> new UserSearchResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3]
                ))
                .collect(Collectors.toList());
    }

    @Override
    public PublicProfileResponse getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        String displayName = profile != null ? profile.getDisplayName() : user.getEmail();
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        String friendshipStatus = determineFriendshipStatus(currentUserId, userId);
        long friendCount = friendshipRepository.countFriendsByUserId(userId);
        return new PublicProfileResponse(userId, user.getEmail(), displayName, avatarUrl,
                                         friendshipStatus, friendCount, friendCount);
    }

    private String determineFriendshipStatus(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) return "NONE";
        if (friendshipRepository.areFriends(currentUserId, targetUserId)) {
            return "FRIENDS";
        }
        Optional<FriendRequest> existing = friendRequestRepository.findBySenderIdAndReceiverId(currentUserId, targetUserId);
        if (existing.isPresent() && existing.get().getStatus() == FriendRequestStatus.PENDING) {
            return "PENDING_SENT";
        }
        Optional<FriendRequest> reverse = friendRequestRepository.findBySenderIdAndReceiverId(targetUserId, currentUserId);
        if (reverse.isPresent() && reverse.get().getStatus() == FriendRequestStatus.PENDING) {
            return "PENDING_RECEIVED";
        }
        return "NONE";
    }

    @Override
    @Transactional
    public FriendRequestDto sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        if (!userRepository.existsById(receiverId)) {
            throw new RuntimeException("Receiver not found");
        }
        if (friendshipRepository.areFriends(senderId, receiverId)) {
            throw new RuntimeException("Already friends");
        }
        if (friendRequestRepository.existsPendingRequestBetween(senderId, receiverId, FriendRequestStatus.PENDING)) {
            throw new RuntimeException("Friend request already pending");
        }
        Optional<FriendRequest> existing = friendRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId);
        if (existing.isPresent() && existing.get().getStatus() == FriendRequestStatus.DECLINED) {
            friendRequestRepository.delete(existing.get());
        }
        existing = friendRequestRepository.findBySenderIdAndReceiverId(receiverId, senderId);
        if (existing.isPresent() && existing.get().getStatus() == FriendRequestStatus.DECLINED) {
            friendRequestRepository.delete(existing.get());
        }
        FriendRequest request = new FriendRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setStatus(FriendRequestStatus.PENDING);
        FriendRequest saved = friendRequestRepository.save(request);
        return toFriendRequestDto(saved);
    }

    @Override
    public List<FriendRequestDto> getIncomingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);
        return requests.stream().map(this::toFriendRequestDto).collect(Collectors.toList());
    }

    @Override
    public List<FriendRequestDto> getOutgoingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequestStatus.PENDING);
        return requests.stream().map(this::toFriendRequestDto).collect(Collectors.toList());
    }

    private FriendRequestDto toFriendRequestDto(FriendRequest request) {
        Long senderId = request.getSenderId();
        User sender = userRepository.findById(senderId).orElse(null);
        Profile profile = sender != null ? profileRepository.findByUserId(senderId).orElse(null) : null;
        String username = sender != null ? sender.getEmail() : "Unknown";
        String displayName = profile != null ? profile.getDisplayName() : username;
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        return new FriendRequestDto(request.getId(), senderId, username, displayName, avatarUrl);
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long requestId, Long currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
        if (!request.getReceiverId().equals(currentUserId)) {
            throw new RuntimeException("Not authorized to accept this request");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }
        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);
        Friendship friendship = new Friendship();
        friendship.setUserId1(request.getSenderId());
        friendship.setUserId2(request.getReceiverId());
        friendshipRepository.save(friendship);
    }

    @Override
    @Transactional
    public void declineFriendRequest(Long requestId, Long currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
        if (!request.getReceiverId().equals(currentUserId)) {
            throw new RuntimeException("Not authorized to decline this request");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }
        request.setStatus(FriendRequestStatus.DECLINED);
        friendRequestRepository.save(request);
    }

    @Override
    public List<FriendDto> getFriends(Long userId) {
        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(userId);
        if (friendIds.isEmpty()) return Collections.emptyList();
        return friendIds.stream().map(this::toFriendDto).collect(Collectors.toList());
    }

    private FriendDto toFriendDto(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        Profile profile = user != null ? profileRepository.findByUserId(userId).orElse(null) : null;
        String username = user != null ? user.getEmail() : "Unknown";
        String displayName = profile != null ? profile.getDisplayName() : username;
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        return new FriendDto(userId, username, displayName, avatarUrl);
    }

    @Override
    public SocialCountsResponse getSocialCounts(Long userId) {
        long friendsCount = friendshipRepository.countFriendsByUserId(userId);
        long pendingRequestsCount = friendRequestRepository.countByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);
        return new SocialCountsResponse(friendsCount, pendingRequestsCount);
    }
}
