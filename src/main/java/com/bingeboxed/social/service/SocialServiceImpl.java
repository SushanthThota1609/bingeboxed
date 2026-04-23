package com.bingeboxed.social.service;

import com.bingeboxed.auth.entity.AuthUser;
import com.bingeboxed.auth.repository.AuthUserRepository;
import com.bingeboxed.profiles.entity.UserProfile;
import com.bingeboxed.profiles.repository.UserProfileRepository;
import com.bingeboxed.shared.client.SocialGraphClient;
import com.bingeboxed.shared.exception.ResourceNotFoundException;
import com.bingeboxed.social.dto.*;
import com.bingeboxed.social.entity.FriendRequest;
import com.bingeboxed.social.entity.Friendship;
import com.bingeboxed.social.repository.FriendRequestRepository;
import com.bingeboxed.social.repository.FriendshipRepository;
import com.bingeboxed.watchlist.entity.WatchlistEntry;
import com.bingeboxed.watchlist.repository.WatchlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SocialServiceImpl implements SocialService, SocialGraphClient {

    private final AuthUserRepository authUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final WatchlistRepository watchlistRepository;

    public SocialServiceImpl(
            AuthUserRepository authUserRepository,
            UserProfileRepository userProfileRepository,
            FriendRequestRepository friendRequestRepository,
            FriendshipRepository friendshipRepository,
            WatchlistRepository watchlistRepository) {
        this.authUserRepository = authUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.watchlistRepository = watchlistRepository;
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
    public List<UserSearchResult> searchUsers(String query, Long currentUserId) {
        String lower = query.toLowerCase();
        List<AuthUser> allUsers = authUserRepository.findAll();
        List<UserSearchResult> results = new ArrayList<>();

        for (AuthUser user : allUsers) {
            if (user.getId().equals(currentUserId)) continue;

            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(user.getId());
            String displayName = profileOpt.map(UserProfile::getDisplayName).orElse("");
            String email = user.getEmail();

            boolean matchesEmail = email.toLowerCase().contains(lower);
            boolean matchesDisplay = displayName != null && displayName.toLowerCase().contains(lower);

            if (!matchesEmail && !matchesDisplay) continue;

            UserSearchResult result = new UserSearchResult();
            result.setUserId(user.getId());
            result.setEmail(email);
            result.setDisplayName(displayName != null ? displayName : "");
            result.setFriendshipStatus(resolveFriendshipStatus(currentUserId, user.getId()));
            results.add(result);
        }

        return results;
    }

    @Override
    public PublicUserProfileResponse getPublicProfile(Long targetUserId, Long currentUserId) {
        AuthUser targetUser = authUserRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(targetUserId);
        String displayName = profileOpt.map(UserProfile::getDisplayName).orElse("");

        long friendCount = friendshipRepository.countByUserId(targetUserId);

        PublicUserProfileResponse response = new PublicUserProfileResponse();
        response.setUserId(targetUserId);
        response.setEmail(targetUser.getEmail());
        response.setDisplayName(displayName != null ? displayName : "");
        response.setFriendshipStatus(resolveFriendshipStatus(currentUserId, targetUserId));
        response.setFollowerCount(friendCount);
        response.setFollowingCount(friendCount);
        return response;
    }

    @Override
    @Transactional
    public FriendRequestDto sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send friend request to yourself");
        }

        if (!authUserRepository.existsById(receiverId)) {
            throw new ResourceNotFoundException("Receiver not found");
        }

        if (friendshipRepository.existsBetweenUsers(senderId, receiverId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already friends");
        }

        Optional<FriendRequest> existing = friendRequestRepository.findBySenderIdAndReceiverId(senderId, receiverId);
        if (existing.isPresent() && existing.get().getStatus() == FriendRequest.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already pending");
        }

        Optional<FriendRequest> reverse = friendRequestRepository.findBySenderIdAndReceiverId(receiverId, senderId);
        if (reverse.isPresent() && reverse.get().getStatus() == FriendRequest.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already pending from the other user");
        }

        FriendRequest request = new FriendRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setStatus(FriendRequest.Status.PENDING);
        FriendRequest saved = friendRequestRepository.save(request);

        return toFriendRequestDto(saved);
    }

    @Override
    public List<FriendRequestDto> getIncomingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository
                .findByReceiverIdAndStatus(userId, FriendRequest.Status.PENDING);
        List<FriendRequestDto> result = new ArrayList<>();
        for (FriendRequest fr : requests) {
            result.add(toFriendRequestDto(fr));
        }
        return result;
    }

    @Override
    public List<FriendRequestDto> getOutgoingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository
                .findBySenderIdAndStatus(userId, FriendRequest.Status.PENDING);
        List<FriendRequestDto> result = new ArrayList<>();
        for (FriendRequest fr : requests) {
            result.add(toFriendRequestDto(fr));
        }
        return result;
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long requestId, Long currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getReceiverId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can accept this request");
        }

        request.setStatus(FriendRequest.Status.ACCEPTED);
        friendRequestRepository.save(request);

        long a = Math.min(request.getSenderId(), request.getReceiverId());
        long b = Math.max(request.getSenderId(), request.getReceiverId());

        if (!friendshipRepository.existsBetweenUsers(a, b)) {
            Friendship friendship = new Friendship();
            friendship.setUserId1(a);
            friendship.setUserId2(b);
            friendshipRepository.save(friendship);
        }
    }

    @Override
    @Transactional
    public void declineFriendRequest(Long requestId, Long currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!request.getReceiverId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can decline this request");
        }

        request.setStatus(FriendRequest.Status.DECLINED);
        friendRequestRepository.save(request);
    }

    @Override
    public List<FriendDto> getFriends(Long userId) {
        List<Long> friendIds = friendshipRepository.findFriendIdsByUserId(userId);
        List<FriendDto> result = new ArrayList<>();

        for (Long friendId : friendIds) {
            authUserRepository.findById(friendId).ifPresent(user -> {
                Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(friendId);
                FriendDto dto = new FriendDto();
                dto.setUserId(friendId);
                dto.setEmail(user.getEmail());
                dto.setDisplayName(profileOpt.map(UserProfile::getDisplayName).orElse(""));
                result.add(dto);
            });
        }

        return result;
    }

    @Override
    public List<PublicWatchlistEntryResponse> getPublicWatchlist(Long targetUserId) {
        if (!authUserRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("User not found");
        }

        List<WatchlistEntry> entries = watchlistRepository.findByUserIdOrderByCreatedAtDesc(targetUserId);
        List<PublicWatchlistEntryResponse> result = new ArrayList<>();

        for (WatchlistEntry entry : entries) {
            PublicWatchlistEntryResponse dto = new PublicWatchlistEntryResponse();
            dto.setId(entry.getId());
            dto.setTmdbId(entry.getTmdbId());
            dto.setContentType(entry.getContentType());
            dto.setStatus(entry.getStatus());
            dto.setTitle(null);
            dto.setRating(null);
            dto.setReview(null);
            result.add(dto);
        }

        return result;
    }

    @Override
    public SocialCountsResponse getSocialCounts(Long userId) {
        long friendsCount = friendshipRepository.countByUserId(userId);
        long pendingCount = friendRequestRepository.countPendingRequestsForUser(userId);
        return new SocialCountsResponse(friendsCount, pendingCount);
    }

    private String resolveFriendshipStatus(Long currentUserId, Long targetUserId) {
        if (friendshipRepository.existsBetweenUsers(currentUserId, targetUserId)) {
            return "FRIENDS";
        }

        Optional<FriendRequest> sent = friendRequestRepository
                .findBySenderIdAndReceiverId(currentUserId, targetUserId);
        if (sent.isPresent() && sent.get().getStatus() == FriendRequest.Status.PENDING) {
            return "PENDING_SENT";
        }

        Optional<FriendRequest> received = friendRequestRepository
                .findBySenderIdAndReceiverId(targetUserId, currentUserId);
        if (received.isPresent() && received.get().getStatus() == FriendRequest.Status.PENDING) {
            return "PENDING_RECEIVED";
        }

        return "NONE";
    }

    private FriendRequestDto toFriendRequestDto(FriendRequest fr) {
        FriendRequestDto dto = new FriendRequestDto();
        dto.setId(fr.getId());
        dto.setSenderId(fr.getSenderId());
        dto.setReceiverId(fr.getReceiverId());
        dto.setStatus(fr.getStatus().name());

        authUserRepository.findById(fr.getSenderId()).ifPresent(sender -> {
            dto.setSenderEmail(sender.getEmail());
            userProfileRepository.findByUserId(sender.getId())
                    .ifPresent(p -> dto.setSenderDisplayName(p.getDisplayName()));
        });

        return dto;
    }
}
