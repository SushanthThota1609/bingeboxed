// src/test/java/com/bingeboxed/reviews/ReviewServiceTest.java
package com.bingeboxed.reviews;

import com.bingeboxed.shared.client.CatalogClient;
import com.bingeboxed.shared.client.CatalogContentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogClient catalogClient;

    private String token;

    private static final int TMDB_CREATE  = 550;
    private static final int TMDB_UPDATE  = 807;
    private static final int TMDB_DELETE  = 680;
    private static final int TMDB_MY      = 13;
    private static final int TMDB_SINGLE  = 278;
    private static final int TMDB_CONTENT = 238;
    private static final int TMDB_CHECK   = 424;
    private static final int TMDB_STATS   = 389;

    @BeforeEach
    void setup() throws Exception {
        // Stub CatalogClient to return a valid content DTO for any tmdbId/type
        CatalogContentDto fakeContent = new CatalogContentDto();
        fakeContent.setTitle("Test Movie");
        fakeContent.setPosterUrl("https://example.com/poster.jpg");
        fakeContent.setReleaseYear(2024);
        fakeContent.setGenre("Drama");
        when(catalogClient.findById(anyInt(), anyString()))
                .thenReturn(Optional.of(fakeContent));

        // Register and login a fresh user per test
        String email = "rv_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\","
                        + "\"password\":\"Test1234!\","
                        + "\"username\":\"rvuser\"}"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\","
                                + "\"password\":\"Test1234!\"}"))
                .andReturn();

        String body = login.getResponse().getContentAsString();
        token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    // ─── FR-01: Create Review ──────────────────────────────────────────────────

    @Test
    void createReview_returns201_withValidInput() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":4,"
                                + "\"reviewText\":\"Great film\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void createReview_returns409_whenAlreadyReviewed() throws Exception {
        String body = "{\"tmdbId\":" + TMDB_CREATE + ","
                + "\"contentType\":\"MOVIE\","
                + "\"rating\":3,"
                + "\"reviewText\":\"Ok\"}";

        // First create
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Duplicate
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createReview_returns400_whenRatingInvalid() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_returns400_whenContentTypeInvalid() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"PODCAST\","
                                + "\"rating\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_returns404_whenContentNotFoundInCatalog() throws Exception {
        when(catalogClient.findById(anyInt(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tmdbId\":" + TMDB_CREATE + ","
                                + "\"contentType\":\"MOVIE\","
                                + "\"rating\":3}"))
                .andExpect(status().isNotFound());
    }

    // ─── FR-02: Update Review ──────────────────────────────────────────────────

    @Test
    void updateReview_returns200_withUpdatedRating() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_UPDATE + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":2,"
                        + "\"reviewText\":\"Meh\"}"));

        mockMvc.perform(put("/api/reviews/" + TMDB_UPDATE)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"reviewText\":\"Changed my mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void updateReview_returns404_whenReviewDoesNotExist() throws Exception {
        mockMvc.perform(put("/api/reviews/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"reviewText\":\"test\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReview_returns400_whenRatingInvalid() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_UPDATE + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":3}"));

        mockMvc.perform(put("/api/reviews/" + TMDB_UPDATE)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0}"))
                .andExpect(status().isBadRequest());
    }

    // ─── FR-03: Delete Review ──────────────────────────────────────────────────

    @Test
    void deleteReview_returns204_onSuccess() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_DELETE + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":3}"));

        mockMvc.perform(delete("/api/reviews/" + TMDB_DELETE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReview_returns404_whenReviewDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/reviews/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ─── FR-04: Get Current User's Reviews ────────────────────────────────────

    @Test
    void getMyReviews_returns200_withEmptyList_forNewUser() throws Exception {
        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getMyReviews_returns200_withReviews_afterCreating() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_MY + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":4}"));

        MvcResult result = mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"tmdbId\":" + TMDB_MY);
    }

    @Test
    void getMyReviews_returns400_whenFilterIsInvalid() throws Exception {
        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + token)
                        .param("minRating", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyReviews_returns400_whenMaxRatingInvalid() throws Exception {
        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + token)
                        .param("maxRating", "6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyReviews_filterByType_returns200() throws Exception {
        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + token)
                        .param("type", "MOVIE"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyReviews_returns400_whenTypeFilterInvalid() throws Exception {
        mockMvc.perform(get("/api/reviews/my")
                        .header("Authorization", "Bearer " + token)
                        .param("type", "ANIME"))
                .andExpect(status().isBadRequest());
    }

    // ─── FR-05: Get Single Review ──────────────────────────────────────────────

    @Test
    void getMyReview_returns200_afterCreating() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_SINGLE + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":5}"));

        mockMvc.perform(get("/api/reviews/my/" + TMDB_SINGLE)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdbId").value(TMDB_SINGLE));
    }

    @Test
    void getMyReview_returns404_whenNotReviewed() throws Exception {
        mockMvc.perform(get("/api/reviews/my/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ─── FR-06: Get Reviews for Content ───────────────────────────────────────

    @Test
    void getContentReviews_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/reviews/content/" + TMDB_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    void getContentReviews_returnsEmptyList_whenNoReviewsExist() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reviews/content/99998"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("[]");
    }

    @Test
    void getContentReviews_includesUserInfo_afterReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_CONTENT + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":3}"));

        MvcResult result = mockMvc.perform(get("/api/reviews/content/" + TMDB_CONTENT))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("userId");
        assertThat(body).contains("displayName");
    }

    // ─── FR-07: Get User's Public Reviews ─────────────────────────────────────

    @Test
    void getPublicUserReviews_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/reviews/user/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicUserReviews_returnsEmptyList_whenUserHasNoReviews() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reviews/user/99999"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("[]");
    }

    @Test
    void getPublicUserReviews_returns400_whenTypeFilterInvalid() throws Exception {
        mockMvc.perform(get("/api/reviews/user/1")
                        .param("type", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    // ─── FR-08: Review Statistics ──────────────────────────────────────────────

    @Test
    void getStats_returns200_withAllFields() throws Exception {
        mockMvc.perform(get("/api/reviews/stats")
                        .header("Authorization", "Bearer " + token))
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
    void getStats_allZero_forNewUser() throws Exception {
        mockMvc.perform(get("/api/reviews/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0));
    }

    @Test
    void getStats_reflectsCreatedReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_STATS + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":5}"));

        mockMvc.perform(get("/api/reviews/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.fiveStarCount").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0));
    }

    // ─── FR-09: Average Rating for Content ────────────────────────────────────

    @Test
    void getContentRating_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/reviews/content/" + TMDB_CONTENT + "/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").exists())
                .andExpect(jsonPath("$.reviewCount").exists());
    }

    @Test
    void getContentRating_returns200WithZero_whenNoReviewsExist() throws Exception {
        mockMvc.perform(get("/api/reviews/content/99997/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviewCount").value(0));
    }

    @Test
    void getContentRating_reflects_submittedReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_CONTENT + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":4}"));

        mockMvc.perform(get("/api/reviews/content/" + TMDB_CONTENT + "/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    // ─── FR-10: Check If Reviewed ──────────────────────────────────────────────

    @Test
    void contains_returns200WithFalse_whenNotReviewed() throws Exception {
        mockMvc.perform(get("/api/reviews/contains/" + TMDB_CHECK)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReviewed").value(false));
    }

    @Test
    void contains_returns200WithTrue_afterReviewing() throws Exception {
        mockMvc.perform(post("/api/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tmdbId\":" + TMDB_CHECK + ","
                        + "\"contentType\":\"MOVIE\","
                        + "\"rating\":4}"));

        mockMvc.perform(get("/api/reviews/contains/" + TMDB_CHECK)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReviewed").value(true))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void contains_ratingIsNull_whenNotReviewed() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reviews/contains/77777")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"rating\":null");
    }

    // ─── FR-11: Reviews Page ───────────────────────────────────────────────────

    @Test
    void reviewsPage_returns200_whenNotAuthenticated() throws Exception {
        // Page always loads — JS redirects to /login client-side
        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    void reviewsPage_returns200_withValidToken() throws Exception {
        mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void reviewsPage_hasNavLinkToCatalog_andWatchlist() throws Exception {
        String html = mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).containsAnyOf("href=\"/catalog\"", "href='/catalog'");
        assertThat(html).containsAnyOf("href=\"/watchlist\"", "href='/watchlist'");
    }

    @Test
    void reviewsPage_hasNavLinkToReviews() throws Exception {
        String html = mockMvc.perform(get("/reviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).containsAnyOf("href=\"/reviews\"", "href='/reviews'");
    }
}