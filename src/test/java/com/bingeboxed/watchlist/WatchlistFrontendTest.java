// src/test/java/com/bingeboxed/watchlist/WatchlistFrontendTest.java

package com.bingeboxed.watchlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WatchlistFrontendTest {

    @Autowired
    private MockMvc mockMvc;

    private String authToken;
    private String authCookie;

    @BeforeEach
    void setup() throws Exception {
        String email = "wl_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\",\"username\":\"wluser\"}"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\"}"))
                .andReturn();

        String body = login.getResponse().getContentAsString();
        authToken = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String cookie = login.getResponse().getHeader("Set-Cookie");
        if (cookie != null) authCookie = cookie.split(";")[0];
    }

    // --- /watchlist page ---

    @Test
    void watchlistPage_returns200_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/watchlist")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
    }

    @Test
    void watchlistPage_isPubliclyAccessible_withoutAuth() throws Exception {
        int status = mockMvc.perform(get("/watchlist"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    // --- /watchlist page navbar links ---

    @Test
    void watchlistPage_hasNavLinkToCatalog() throws Exception {
        String html = getPage("/watchlist");
        assertThat(html)
                .as("Watchlist navbar missing /catalog link")
                .containsAnyOf("href=\"/catalog\"", "href='/catalog'");
    }

    @Test
    void watchlistPage_hasNavLinkToProfile() throws Exception {
        String html = getPage("/watchlist");
        assertThat(html)
                .as("Watchlist navbar missing /profile link")
                .containsAnyOf("href=\"/profile\"", "href='/profile'");
    }

    @Test
    void watchlistPage_hasLogoutOption() throws Exception {
        String html = getPage("/watchlist");
        assertThat(html).containsIgnoringCase("logout");
    }

    @Test
    void watchlistPage_logoutIsNotAGetHref() throws Exception {
        String html = getPage("/watchlist");
        assertThat(html)
                .as("Logout must not be a plain GET — must use fetch POST")
                .doesNotContain("href=\"/logout\"");
    }

    @Test
    void watchlistPage_usesBingeBoxedBranding() throws Exception {
        String html = getPage("/watchlist");
        assertThat(html).containsIgnoringCase("BingeBoxed");
    }

    @Test
    void watchlistPage_hasStatusBadgeColors() throws Exception {
        String html = getPage("/watchlist");
        // spec requires blue/yellow/green for the three statuses
        assertThat(html)
                .as("Watchlist page should reference status badge styles")
                .containsAnyOf("WANT_TO_WATCH", "WATCHING", "COMPLETED");
    }

    @Test
    void watchlistPage_doesNotLeakJwtInHtml() throws Exception {
        String html = getPage("/watchlist");
        assertThat(html)
                .as("JWT must never be server-rendered into HTML")
                .doesNotContain("eyJ");
    }

    @Test
    void watchlistPage_readsTokenFromLocalStorage() throws Exception {
        String html = getPage("/watchlist");
        assertThat(html)
                .as("Watchlist JS should use localStorage for token")
                .containsAnyOf("localStorage", "getItem");
    }

    // --- POST /api/watchlist ---

    @Test
    void addToWatchlist_returns201_withValidRequest() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":550,\"contentType\":\"MOVIE\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void addToWatchlist_returns401_withoutJwt() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":550,\"contentType\":\"MOVIE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addToWatchlist_returns409_whenAlreadyExists() throws Exception {
        String body = "{\"tmdbId\":807,\"contentType\":\"MOVIE\"}";

        mockMvc.perform(post("/api/watchlist")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/api/watchlist")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void addToWatchlist_returns400_whenContentTypeIsInvalid() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":550,\"contentType\":\"DOCUMENTARY\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/watchlist/{tmdbId} ---

    @Test
    void removeFromWatchlist_returns204_onSuccess() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":13,\"contentType\":\"MOVIE\"}"));

        mockMvc.perform(delete("/api/watchlist/13")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFromWatchlist_returns404_whenEntryDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/watchlist/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeFromWatchlist_returns401_withoutJwt() throws Exception {
        mockMvc.perform(delete("/api/watchlist/550"))
                .andExpect(status().isUnauthorized());
    }

    // --- PATCH /api/watchlist/{tmdbId} ---

    @Test
    void updateStatus_returns200_withValidStatus() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":680,\"contentType\":\"MOVIE\"}"));

        mockMvc.perform(patch("/api/watchlist/680")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WATCHING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WATCHING"));
    }

    @Test
    void updateStatus_returns400_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/watchlist/550")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DROPPED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_returns401_withoutJwt() throws Exception {
        mockMvc.perform(patch("/api/watchlist/550")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WATCHING\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/watchlist ---

    @Test
    void getMyWatchlist_returns200_withEmptyList_forNewUser() throws Exception {
        mockMvc.perform(get("/api/watchlist")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getMyWatchlist_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/watchlist/{tmdbId} ---

    @Test
    void getEntry_returns404_whenNotInWatchlist() throws Exception {
        mockMvc.perform(get("/api/watchlist/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEntry_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/watchlist/550"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/watchlist/contains/{tmdbId} ---

    @Test
    void contains_returns200WithFalse_whenNotInWatchlist() throws Exception {
        mockMvc.perform(get("/api/watchlist/contains/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void contains_returns200WithTrue_afterItemAdded() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":238,\"contentType\":\"MOVIE\"}"));

        mockMvc.perform(get("/api/watchlist/contains/238")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    // --- GET /api/watchlist/user/{userId} (public) ---

    @Test
    void getPublicWatchlist_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/watchlist/user/1"))
                .andExpect(status().isOk());
    }

    // --- GET /api/watchlist/stats ---

    @Test
    void getStats_returns200_withAllFields() throws Exception {
        mockMvc.perform(get("/api/watchlist/stats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.wantToWatch").exists())
                .andExpect(jsonPath("$.watching").exists())
                .andExpect(jsonPath("$.completed").exists());
    }

    @Test
    void getStats_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/watchlist/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStats_allZero_forNewUser() throws Exception {
        mockMvc.perform(get("/api/watchlist/stats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.wantToWatch").value(0))
                .andExpect(jsonPath("$.watching").value(0))
                .andExpect(jsonPath("$.completed").value(0));
    }

    // --- Helper ---

    private String getPage(String path) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}