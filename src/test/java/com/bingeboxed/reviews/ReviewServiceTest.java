// src/test/java/com/bingeboxed/reviews/ReviewServiceTest.java
package com.bingeboxed.reviews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ReviewServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private Long testUserId;

    // Different tmdb ids per test group to avoid 409 conflicts between tests
    private static final int TMDB_CREATE   = 550;      // Fight Club
    private static final int TMDB_UPDATE   = 807;      // Se7en
    private static final int TMDB_DELETE   = 680;      // Pulp Fiction
    private static final int TMDB_MY       = 13;       // Forrest Gump
    private static final int TMDB_SINGLE   = 278;      // The Shawshank Redemption
    private static final int TMDB_CONTENT  = 238;      // The Godfather
    private static final int TMDB_CHECK    = 424;      // Schindler's List
    private static final int TMDB_STATS    = 389;      // 12 Angry Men
    private static final int TMDB_SERIES   = 1396;     // Breaking Bad
    private static final int TMDB_USER_INFO_TEST = 9991; // Test content for user info

    @BeforeEach
    void setup() throws Exception {
        String uniqueEmail = "review_test_" + System.currentTimeMillis() + "@test.com";

        // Register user
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + uniqueEmail + "\","
                        + "\"password\":\"Test1234!\","
                        + "\"username\":\"reviewuser\"}"));

        // Login to get token
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail + "\","
                                + "\"password\":\"Test1234!\"}"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        authToken = jsonNode.get("token").asText();
    }

    // ─── FR-01: Create Review ─────────────────────────────────────────────────

    @Test
    void createReview_returns201_withValidMovieReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":4,"
                                + "\"reviewText\":\"Great film! Highly recommend.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.reviewText").value("Great film! Highly recommend."))
                .andExpect(jsonPath("$.contentType").value("MOVIE"));
    }

    @Test
    void createReview_returns201_withValidSeriesReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_SERIES + ","
                                + "\"contentType\":\"SERIES\","
                                + "\"rating\":5,"
                                + "\"reviewText\":\"Best series ever!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.contentType").value("SERIES"));
    }

    @Test
    void createReview_returns201_withoutReviewText() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(3));
    }

    @Test
    void createReview_returns400_withInvalidContentType() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"INVALID\","
                                + "\"rating\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_returns400_withRatingTooLow() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_returns400_withRatingTooHigh() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_returns409_whenAlreadyReviewed() throws Exception {
        String requestBody = "{\"tmdbId\":" + TMDB_CREATE + ","
                + "\"contentType\":\"MOVIE\","
                + "\"rating\":3,"
                + "\"reviewText\":\"Ok movie\"}";

        // First review - should succeed
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated());

        // Second review - should fail with conflict
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    // ─── FR-02: Update Review ─────────────────────────────────────────────────

    @Test
    void updateReview_returns200_withUpdatedRating() throws Exception {
        // Create review first
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_UPDATE + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":2,"
                        + "\"reviewText\":\"Not great\"}"))
                .andExpect(status().isCreated());

        // Update review
        mockMvc.perform(put("/api/reviews/" + TMDB_UPDATE)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"reviewText\":\"Actually amazing!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewText").value("Actually amazing!"));
    }

    @Test
    void updateReview_returns404_whenReviewDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/reviews/99999")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"reviewText\":\"test\"}"))
                .andExpect(status().isNotFound());
    }

    // ─── FR-03: Delete Review ─────────────────────────────────────────────────

    @Test
    void deleteReview_returns204_onSuccess() throws Exception {
        // Create review first
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_DELETE + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":3}"))
                .andExpect(status().isCreated());

        // Delete review
        mockMvc.perform(delete("/api/reviews/" + TMDB_DELETE)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/reviews/my/" + TMDB_DELETE)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReview_returns404_whenReviewDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/reviews/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    // ─── FR-04: Get Current User's Reviews ───────────────────────────────────

    @Test
    void getMyReviews_returns200_withEmptyList_forNewUser() throws Exception {
        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMyReviews_returns200_withReviews() throws Exception {
        // Create a review
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_MY + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":4}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tmdbId").value(TMDB_MY))
                .andExpect(jsonPath("$[0].rating").value(4));
    }

    @Test
    void getMyReviews_filtersByType() throws Exception {
        // Create movie review
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_MY + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":4}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + authToken)
                        .param("type", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─── FR-05: Get Single Review by Current User ─────────────────────────────

    @Test
    void getMyReview_returns200_afterCreating() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_SINGLE + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":5,"
                        + "\"reviewText\":\"Masterpiece!\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/my/" + TMDB_SINGLE)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdbId").value(TMDB_SINGLE))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.reviewText").value("Masterpiece!"));
    }

    @Test
    void getMyReview_returns404_whenNotReviewed() throws Exception {
        mockMvc.perform(get("/api/reviews/my/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    // ─── FR-06: Get Reviews for Specific Content ──────────────────────────────

    @Test
    void getContentReviews_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/reviews/content/" + TMDB_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    void getContentReviews_returnsUserInfo_whenReviewsExist() throws Exception {
        // Create a review first
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_USER_INFO_TEST + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":5,"
                        + "\"reviewText\":\"Test review for user info\"}"))
                .andExpect(status().isCreated());

        // Now get content reviews - should have user info
        mockMvc.perform(get("/api/reviews/content/" + TMDB_USER_INFO_TEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.userId").exists())
                .andExpect(jsonPath("$[0].user.displayName").exists());
    }

    @Test
    void getContentReviews_returnsEmptyList_whenNoReviewsExist() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reviews/content/99998"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("[]");
    }

    // ─── FR-07: Get User's Public Reviews ────────────────────────────────────

    @Test
    void getPublicUserReviews_returns200_withoutAuth() throws Exception {
        // First create current user's ID by getting profile
        MvcResult profileResult = mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + authToken))
                .andReturn();
        String profileBody = profileResult.getResponse().getContentAsString();
        JsonNode profileNode = objectMapper.readTree(profileBody);
        Long currentUserId = profileNode.get("id").asLong();
        
        mockMvc.perform(get("/api/reviews/user/" + currentUserId))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicUserReviews_returnsEmptyList_whenUserHasNoReviews() throws Exception {
        mockMvc.perform(get("/api/reviews/user/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPublicUserReviews_filtersByType() throws Exception {
        mockMvc.perform(get("/api/reviews/user/1")
                        .param("type", "MOVIE"))
                .andExpect(status().isOk());
    }

    // ─── FR-08: Review Statistics ─────────────────────────────────────────────

    @Test
    void getStats_returns200_withAllFields() throws Exception {
        mockMvc.perform(get("/api/reviews/stats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").exists())
                .andExpect(jsonPath("$.averageRating").exists())
                .andExpect(jsonPath("$.fiveStarCount").exists())
                .andExpect(jsonPath("$.fourStarCount").exists())
                .andExpect(jsonPath("$.threeStarCount").exists())
                .andExpect(jsonPath("$.twoStarCount").exists())
                .andExpect(jsonPath("$.oneStarCount").exists());
    }

    @Test
    void getStats_returnsAllZero_forNewUser() throws Exception {
        mockMvc.perform(get("/api/reviews/stats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.fiveStarCount").value(0))
                .andExpect(jsonPath("$.fourStarCount").value(0));
    }

    @Test
    void getStats_updatesAfterCreatingReview() throws Exception {
        // Create a review
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_STATS + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":5,"
                        + "\"reviewText\":\"Perfect!\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/stats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.fiveStarCount").value(1));
    }

    // ─── FR-09: Get Average Rating for Content ────────────────────────────────

    @Test
    void getContentRating_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/reviews/content/" + TMDB_CONTENT + "/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").exists())
                .andExpect(jsonPath("$.reviewCount").exists());
    }

    @Test
    void getContentRating_returns200WithZero_whenNoReviewsExist() throws Exception {
        // Always returns 200, never 404
        mockMvc.perform(get("/api/reviews/content/99997/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }

    // ─── FR-10: Check If User Reviewed Content ────────────────────────────────

    @Test
    void contains_returns200WithFalse_whenNotReviewed() throws Exception {
        mockMvc.perform(get("/api/reviews/contains/" + TMDB_CHECK)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReviewed").value(false));
    }

    @Test
    void contains_returns200WithTrue_afterReviewing() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_CHECK + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":4}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/contains/" + TMDB_CHECK)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReviewed").value(true))
                .andExpect(jsonPath("$.rating").value(4));
    }

    // ─── FR-11: Reviews Page (Thymeleaf) ─────────────────────────────────────

    @Test
    void reviewsPage_returns200_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
    }

    @Test
    void reviewsPage_hasNavLinkToCatalog_andWatchlist_andProfile() throws Exception {
        String html = mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("href=\"/catalog\"");
        assertThat(html).contains("href=\"/watchlist\"");
        assertThat(html).contains("href=\"/reviews\"");
        assertThat(html).contains("href=\"/profile\"");
    }

    // ─── Additional Security Tests ───────────────────────────────────────────

    @Test
    void createReview_returns401_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":4}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateReview_returns401_whenNoToken() throws Exception {
        mockMvc.perform(put("/api/reviews/" + TMDB_UPDATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReview_returns401_whenNoToken() throws Exception {
        mockMvc.perform(delete("/api/reviews/" + TMDB_DELETE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyReviews_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/reviews/my"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Integration: Complete Review Flow ───────────────────────────────────

    @Test
    void completeReviewFlow_createUpdateDelete() throws Exception {
        int tmdbId = 9992;
        
        // 1. Create review
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + tmdbId + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":3,"
                                + "\"reviewText\":\"Initial review\"}"))
                .andExpect(status().isCreated());
        
        // 2. Verify it exists
        mockMvc.perform(get("/api/reviews/my/" + tmdbId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(3));
        
        // 3. Update review
        mockMvc.perform(put("/api/reviews/" + tmdbId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"reviewText\":\"Updated review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
        
        // 4. Verify public can see it
        mockMvc.perform(get("/api/reviews/content/" + tmdbId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5));
        
        // 5. Delete review
        mockMvc.perform(delete("/api/reviews/" + tmdbId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
        
        // 6. Verify it's gone
        mockMvc.perform(get("/api/reviews/my/" + tmdbId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}