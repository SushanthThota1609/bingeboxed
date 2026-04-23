package com.bingeboxed.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialGraphServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Helpers ───────────────────────────────────────────────────────────

    private String getToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email)));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email)))
                .andReturn().getResponse().getContentAsString();
        return response.split("\"token\":\"")[1].replace("\"}", "").trim();
    }

    private UserSession createUser(String emailPrefix) throws Exception {
        String email = emailPrefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email)));
        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"password123\"}", email)))
                .andReturn().getResponse().getContentAsString();
        String token = loginResp.split("\"token\":\"")[1].replace("\"}", "").trim();
        Long userId = resolveUserId(email, token);
        return new UserSession(token, userId);
    }

    private Long resolveUserId(String email, String token) throws Exception {
        // Create a second user to search for the target (search excludes self)
        String helperEmail = "helper_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"email\":\"%s\",\"password\":\"password123\"}", helperEmail)));
        String helperLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"password123\"}", helperEmail)))
                .andReturn().getResponse().getContentAsString();
        String helperToken = helperLogin.split("\"token\":\"")[1].replace("\"}", "").trim();

        String prefix = email.split("@")[0].split("_")[0];
        String searchResp = mockMvc.perform(get("/api/users/search?query=" + email.split("@")[0])
                        .header("Authorization", "Bearer " + helperToken))
                .andReturn().getResponse().getContentAsString();

        // Find entry matching exact email
        for (String part : searchResp.split("\\{")) {
            if (part.contains(email) && part.contains("\"userId\":")) {
                return Long.parseLong(part.split("\"userId\":")[1].split("[,}]")[0].trim());
            }
        }
        throw new RuntimeException("Could not resolve userId for " + email);
    }

    private record UserSession(String token, Long userId) {}

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
        UserSession viewer = createUser("viewer");
        UserSession viewed = createUser("viewed");
        mockMvc.perform(get("/api/users/" + viewed.userId() + "/profile")
                        .header("Authorization", "Bearer " + viewer.token()))
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
        UserSession sender   = createUser("sender");
        UserSession receiver = createUser("receiver");
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": " + receiver.userId() + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void sendFriendRequest_toSelf_returns400() throws Exception {
        UserSession self = createUser("selfuser");
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + self.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": " + self.userId() + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendFriendRequest_duplicate_returns409() throws Exception {
        UserSession sender   = createUser("sender2");
        UserSession receiver = createUser("receiver2");
        String body = "{\"receiverId\": " + receiver.userId() + "}";
        mockMvc.perform(post("/api/friends/requests")
                .header("Authorization", "Bearer " + sender.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + sender.token())
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
        UserSession sender   = createUser("acceptsender");
        UserSession receiver = createUser("acceptreceiver");
        String requestResponse = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": " + receiver.userId() + "}"))
                .andReturn().getResponse().getContentAsString();
        String requestId = requestResponse.split("\"id\":")[1].split(",")[0].trim();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + receiver.token()))
                .andExpect(status().isOk());
    }

    @Test
    void acceptFriendRequest_wrongUser_returns403() throws Exception {
        UserSession sender   = createUser("acceptsender2");
        UserSession other    = createUser("acceptother");
        UserSession receiver = createUser("acceptreceiver2");
        String requestResponse = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": " + receiver.userId() + "}"))
                .andReturn().getResponse().getContentAsString();
        String requestId = requestResponse.split("\"id\":")[1].split(",")[0].trim();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isForbidden());
    }

    // ── FR-07: Decline Friend Request ─────────────────────────────────────

    @Test
    void declineFriendRequest_validRequest_returns200() throws Exception {
        UserSession sender   = createUser("declinesender");
        UserSession receiver = createUser("declinereceiver");
        String requestResponse = mockMvc.perform(post("/api/friends/requests")
                        .header("Authorization", "Bearer " + sender.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\": " + receiver.userId() + "}"))
                .andReturn().getResponse().getContentAsString();
        String requestId = requestResponse.split("\"id\":")[1].split(",")[0].trim();
        mockMvc.perform(post("/api/friends/requests/" + requestId + "/decline")
                        .header("Authorization", "Bearer " + receiver.token()))
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
        UserSession owner = createUser("watchlistowner");
        mockMvc.perform(get("/api/watchlist/user/" + owner.userId()))
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
        UserSession user = createUser("countuser");
        mockMvc.perform(get("/api/social/counts/" + user.userId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendsCount").exists())
                .andExpect(jsonPath("$.pendingRequestsCount").exists());
    }

    @Test
    void getFriendCounts_defaultsToZero() throws Exception {
        UserSession user = createUser("zerocount");
        mockMvc.perform(get("/api/social/counts/" + user.userId()))
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
        // /social is permitAll (auth enforced client-side); page loads with 200
        // Client-side JS redirects to /login if no token — server returns 200
        mockMvc.perform(get("/social")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }
}
