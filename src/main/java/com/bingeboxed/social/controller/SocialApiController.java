package com.bingeboxed.social.controller;

import com.bingeboxed.shared.user.UserResolverService;
import com.bingeboxed.social.dto.*;
import com.bingeboxed.social.service.SocialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class SocialApiController {

    private final SocialService socialService;
    private final UserResolverService userResolverService;

    public SocialApiController(SocialService socialService, UserResolverService userResolverService) {
        this.socialService = socialService;
        this.userResolverService = userResolverService;
    }

    @GetMapping("/api/users/search")
    public ResponseEntity<List<UserSearchResult>> searchUsers(
            @RequestParam String query,
            Authentication auth) {
        if (!StringUtils.hasText(query)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query must not be blank");
        }
        Long currentUserId = resolveCurrentUserId(auth);
        return ResponseEntity.ok(socialService.searchUsers(query, currentUserId));
    }

    @GetMapping("/api/users/{userId}/profile")
    public ResponseEntity<PublicUserProfileResponse> getPublicProfile(
            @PathVariable Long userId,
            Authentication auth) {
        Long currentUserId = resolveCurrentUserId(auth);
        return ResponseEntity.ok(socialService.getPublicProfile(userId, currentUserId));
    }

    @PostMapping("/api/friends/requests")
    public ResponseEntity<FriendRequestDto> sendFriendRequest(
            @RequestBody SendFriendRequestBody body,
            Authentication auth) {
        Long senderId = resolveCurrentUserId(auth);
        FriendRequestDto dto = socialService.sendFriendRequest(senderId, body.getReceiverId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/api/friends/requests/incoming")
    public ResponseEntity<List<FriendRequestDto>> getIncomingRequests(Authentication auth) {
        Long userId = resolveCurrentUserId(auth);
        return ResponseEntity.ok(socialService.getIncomingRequests(userId));
    }

    @GetMapping("/api/friends/requests/outgoing")
    public ResponseEntity<List<FriendRequestDto>> getOutgoingRequests(Authentication auth) {
        Long userId = resolveCurrentUserId(auth);
        return ResponseEntity.ok(socialService.getOutgoingRequests(userId));
    }

    @PostMapping("/api/friends/requests/{requestId}/accept")
    public ResponseEntity<Void> acceptFriendRequest(
            @PathVariable Long requestId,
            Authentication auth) {
        Long currentUserId = resolveCurrentUserId(auth);
        socialService.acceptFriendRequest(requestId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/friends/requests/{requestId}/decline")
    public ResponseEntity<Void> declineFriendRequest(
            @PathVariable Long requestId,
            Authentication auth) {
        Long currentUserId = resolveCurrentUserId(auth);
        socialService.declineFriendRequest(requestId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/friends")
    public ResponseEntity<List<FriendDto>> getFriends(Authentication auth) {
        Long userId = resolveCurrentUserId(auth);
        return ResponseEntity.ok(socialService.getFriends(userId));
    }

    @GetMapping("/api/social/counts/{userId}")
    public ResponseEntity<SocialCountsResponse> getSocialCounts(@PathVariable Long userId) {
        return ResponseEntity.ok(socialService.getSocialCounts(userId));
    }

    private Long resolveCurrentUserId(Authentication auth) {
        String email = auth.getName();
        return userResolverService.resolveUserId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
