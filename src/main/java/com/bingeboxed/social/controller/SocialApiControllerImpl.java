package com.bingeboxed.social.controller;

import com.bingeboxed.auth.repository.UserRepository;
import com.bingeboxed.shared.security.JwtService;
import com.bingeboxed.social.dto.*;
import com.bingeboxed.social.service.SocialService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SocialApiControllerImpl {

    private final SocialService socialService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public SocialApiControllerImpl(SocialService socialService,
                                   JwtService jwtService,
                                   UserRepository userRepository) {
        this.socialService = socialService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid token");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String query,
                                                                 HttpServletRequest request) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Long currentUserId = getCurrentUserId(request);
        List<UserSearchResponse> results = socialService.searchUsers(query, currentUserId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<PublicProfileResponse> getUserProfile(@PathVariable Long userId,
                                                                HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        PublicProfileResponse profile = socialService.getUserProfile(userId, currentUserId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/friends/requests")
    public ResponseEntity<FriendRequestDto> sendFriendRequest(@RequestBody SendFriendRequestRequest body,
                                                              HttpServletRequest request) {
        Long senderId = getCurrentUserId(request);
        Long receiverId = body.getReceiverId();
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        FriendRequestDto dto = socialService.sendFriendRequest(senderId, receiverId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.getId())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping("/friends/requests/incoming")
    public ResponseEntity<List<FriendRequestDto>> getIncomingRequests(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<FriendRequestDto> requests = socialService.getIncomingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/friends/requests/outgoing")
    public ResponseEntity<List<FriendRequestDto>> getOutgoingRequests(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<FriendRequestDto> requests = socialService.getOutgoingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    public ResponseEntity<Void> acceptFriendRequest(@PathVariable Long requestId,
                                                    HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        socialService.acceptFriendRequest(requestId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/friends/requests/{requestId}/decline")
    public ResponseEntity<Void> declineFriendRequest(@PathVariable Long requestId,
                                                     HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        socialService.declineFriendRequest(requestId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/friends")
    public ResponseEntity<List<FriendDto>> getFriends(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<FriendDto> friends = socialService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/social/counts/{userId}")
    public ResponseEntity<SocialCountsResponse> getSocialCounts(@PathVariable Long userId) {
        SocialCountsResponse counts = socialService.getSocialCounts(userId);
        return ResponseEntity.ok(counts);
    }
}
