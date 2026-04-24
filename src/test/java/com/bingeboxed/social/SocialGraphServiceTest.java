package com.bingeboxed.social;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialGraphServiceTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Helper ────────────────────────────────────────────────────────────

    private String getToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"email\":\"%s\",\"password\":\"password123\"}", email)));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"password123\"}", email)))
                .andReturn().getResponse().getContentAsString();
        return response.split("\"token\":\"")[1]
                .replace("\"}", "").trim();
    }

    // ── FR-01: User Search ────────────────────────────────────────────────

    @Test
    void searchUsers_validQuery_returns200() throws Exception {
        String token = getToken("searcher@test.com");
        getToken("findme@test.com");
        mockMvc.perform(get("/api/users/search?query=findme")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchUsers_blankQuery_returns400() throws Exception {
        String token = getToken("searcher2@test.com");
        mockMvc.perform(get("/api/users/search?query=")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchUsers_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/search?query=test"))
                .andExpect(status().isUnauthorized());
    }

    // ── FR-02: View Public User Profile ───────────────────────────────────

    @Test
    void viewProfile_existingUser_returns200() throws Exception {
        String token = getToken("viewer@test.com");
        getToken("viewed@test.com");
        mockMvc.perform(get("/api/users/2/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendshipStatus").exists());
    }

    @Test
    void viewProfile_nonExistentUser_returns404() throws Exception {
        String token = getToken("viewer2@test.com");
        mockMvc.perform(get("/api/users/99999/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── FR-03: Send Friend Request ────────────────────────────────────────

    @Test
    void sendFriendRequest_validRequest_returns201() throws Exception {
        String senderToken = getToken("sender@test.com");
        getToken("receiver@test.com");
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": 2}"))
                .andExpect(status().isCreated());
    }

    @Test
    void sendFriendRequest_toSelf_returns400() throws Exception {
        String token = getToken("selfuser@test.com");
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": 1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendFriendRequest_duplicate_returns409() throws Exception {
        String senderToken = getToken("sender2@test.com");
        getToken("receiver2@test.com");
        String body = "{\"receiverId\": 2}";
        mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + senderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ── FR-04: Get Incoming Friend Requests ───────────────────────────────

    @Test
    void getIncomingRequests_authenticated_returns200() throws Exception {
        String token = getToken("incoming@test.com");
        mockMvc.perform(get("/api/friends/requests/incoming")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getIncomingRequests_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/friends/requests/incoming"))
                .andExpect(status().isUnauthorized());
    }

    // ── FR-05: Get Outgoing Friend Requests ───────────────────────────────

    @Test
    void getOutgoingRequests_authenticated_returns200() throws Exception {
        String token = getToken("outgoing@test.com");
        mockMvc.perform(get("/api/friends/requests/outgoing")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getOutgoingRequests_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/friends/requests/outgoing"))
                .andExpect(status().isUnauthorized());
    }

    // ── FR-06: Accept Friend Request ──────────────────────────────────────

    @Test
    void acceptFriendRequest_validRequest_returns200() throws Exception {
        String senderToken = getToken("acceptsender@test.com");
        String receiverToken = getToken("acceptreceiver@test.com");
        String requestResponse = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": 2}"))
                .andReturn().getResponse().getContentAsString();
        String requestId = requestResponse.split("\"id\":")[1].split(",")[0].trim();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk());
    }

    @Test
    void acceptFriendRequest_wrongUser_returns403() throws Exception {
        String senderToken = getToken("acceptsender2@test.com");
        String otherToken = getToken("acceptother@test.com");
        getToken("acceptreceiver2@test.com");
        String requestResponse = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": 3}"))
                .andReturn().getResponse().getContentAsString();
        String requestId = requestResponse.split("\"id\":")[1].split(",")[0].trim();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    // ── FR-07: Decline Friend Request ─────────────────────────────────────

    @Test
    void declineFriendRequest_validRequest_returns200() throws Exception {
        String senderToken = getToken("declinesender@test.com");
        String receiverToken = getToken("declinereceiver@test.com");
        String requestResponse = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": 2}"))
                .andReturn().getResponse().getContentAsString();
        String requestId = requestResponse.split("\"id\":")[1].split(",")[0].trim();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/decline")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk());
    }

    @Test
    void declineFriendRequest_notFound_returns404() throws Exception {
        String token = getToken("declineother@test.com");
        mockMvc.perform(post("/api/friends/requests/99999/decline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── FR-08: Get Friends List ───────────────────────────────────────────

    @Test
    void getFriendsList_authenticated_returns200() throws Exception {
        String token = getToken("friendslist@test.com");
        mockMvc.perform(get("/api/friends")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getFriendsList_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized());
    }

    // ── FR-09: Get Another User's Public Watchlist ────────────────────────

    @Test
    void getPublicWatchlist_noAuth_returns200() throws Exception {
        getToken("watchlistowner@test.com");
        mockMvc.perform(get("/api/watchlist/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPublicWatchlist_nonExistentUser_returns404() throws Exception {
        mockMvc.perform(get("/api/watchlist/user/99999"))
                .andExpect(status().isNotFound());
    }

    // ── FR-10: Get Friend Counts ──────────────────────────────────────────

    @Test
    void getFriendCounts_existingUser_returns200() throws Exception {
        getToken("countuser@test.com");
        mockMvc.perform(get("/api/social/counts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendsCount").exists())
                .andExpect(jsonPath("$.pendingRequestsCount").exists());
    }

    @Test
    void getFriendCounts_defaultsToZero() throws Exception {
        getToken("zerocount@test.com");
        mockMvc.perform(get("/api/social/counts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendsCount").value(0));
    }

    // ── FR-11: Social Page ────────────────────────────────────────────────

    @Test
    void socialPage_authenticated_returns200() throws Exception {
        String token = getToken("socialpage@test.com");
        mockMvc.perform(get("/social")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(view().name("social/index"));
    }

    @Test
    void socialPage_noAuth_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/social"))
                .andExpect(status().is3xxRedirection());
    }
}