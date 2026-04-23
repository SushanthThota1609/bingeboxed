package com.bingeboxed.watchlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that fire REAL HTTP requests through the full Spring
 * Security → Controller → Service → Repository pipeline.
 *
 * WHY THESE TESTS EXIST:
 * All Mockito-based tests in WatchlistServiceTest pass because they mock
 * UserResolver and never exercise the JWT extraction → UserResolverImpl →
 * DataSource chain. The console shows every request returning 500
 * "Failed to resolve user" — which only appears in the full pipeline.
 *
 * These tests will FAIL on the bug and PASS once it is fixed.
 *
 * Prerequisites:
 *  - application-test.properties pointing at an H2 in-memory datasource
 *  - h2 dependency in pom.xml (scope: test)
 *  - A valid JWT signed with the same secret as the test profile
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Watchlist Integration Tests — Full HTTP Pipeline")
class WatchlistIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.bingeboxed.shared.security.JwtService jwtService;

    private String validToken;
    private static final String TEST_EMAIL    = "ssher9@uic.edu";
    private static final String TEST_PASSWORD = "password123";
    private static final int    TMDB_ID       = 936075;

    // ─── Setup ───────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Insert a real user into the DB — same schema as production
        // (id, created_at, email, password)
        jdbcTemplate.execute("DELETE FROM watchlist_entries");
        jdbcTemplate.execute("DELETE FROM auth_users");
        jdbcTemplate.update(
                "INSERT INTO auth_users (email, password, created_at) VALUES (?, ?, NOW())",
                TEST_EMAIL,
                new BCryptPasswordEncoder().encode(TEST_PASSWORD)
        );

        // Generate a real JWT for this user — the same way the auth service would
        validToken = jwtService.generateToken(TEST_EMAIL);
    }

    // =========================================================================
    // REGRESSION Bug #2: POST /api/watchlist → 500 "Failed to resolve user"
    //
    // The browser console shows POST http://localhost:8080/api/watchlist
    // returning 500. This means the full pipeline: JWT extraction →
    // UserResolverImpl → DataSource → SQL breaks somewhere.
    // =========================================================================

    @Nested
    @DisplayName("REGRESSION: POST /api/watchlist — 500 on Add to Watchlist")
    class AddToWatchlistIntegrationTests {

        @Test
        @DisplayName("POST /api/watchlist with valid JWT must NOT return 500")
        void postWatchlist_validJwt_doesNotReturn500() throws Exception {
            mockMvc.perform(post("/api/watchlist")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tmdbId", TMDB_ID,
                                    "contentType", "MOVIE"
                            ))))
                    // 500 = bug still present. 201 = fixed. 409 = already exists (also fine).
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(500, result.getResponse().getStatus(), "Expected non-500 but got Internal Server Error"));
        }

        @Test
        @DisplayName("POST /api/watchlist with valid JWT returns 201 CREATED")
        void postWatchlist_validJwt_returns201() throws Exception {
            mockMvc.perform(post("/api/watchlist")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tmdbId", TMDB_ID,
                                    "contentType", "MOVIE"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tmdbId", is(TMDB_ID)))
                    .andExpect(jsonPath("$.status", is("WANT_TO_WATCH")));
        }

        @Test
        @DisplayName("POST /api/watchlist without Authorization header returns 401 not 500")
        void postWatchlist_noToken_returns401() throws Exception {
            mockMvc.perform(post("/api/watchlist")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tmdbId", TMDB_ID,
                                    "contentType", "MOVIE"
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/watchlist with invalid/expired JWT returns 401 not 500")
        void postWatchlist_invalidToken_returns401() throws Exception {
            mockMvc.perform(post("/api/watchlist")
                            .header("Authorization", "Bearer invalid.token.here")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tmdbId", TMDB_ID,
                                    "contentType", "MOVIE"
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/watchlist duplicate entry returns 409 not 500")
        void postWatchlist_duplicate_returns409() throws Exception {
            // First add
            mockMvc.perform(post("/api/watchlist")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tmdbId", TMDB_ID,
                                    "contentType", "MOVIE"
                            ))))
                    .andExpect(status().isCreated());

            // Second add — must be 409, not 500
            mockMvc.perform(post("/api/watchlist")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tmdbId", TMDB_ID,
                                    "contentType", "MOVIE"
                            ))))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    // REGRESSION: GET /api/watchlist/contains/{tmdbId} → 500
    // Console shows: Failed to load resource: 500 /api/watchlist/contains/936075
    // =========================================================================

    @Nested
    @DisplayName("REGRESSION: GET /api/watchlist/contains/{tmdbId} — 500")
    class ContainsIntegrationTests {

        @Test
        @DisplayName("GET /api/watchlist/contains/{tmdbId} with valid JWT must NOT return 500")
        void containsCheck_validJwt_doesNotReturn500() throws Exception {
            mockMvc.perform(get("/api/watchlist/contains/" + TMDB_ID)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(500, result.getResponse().getStatus(), "Expected non-500 but got Internal Server Error"));
        }

        @Test
        @DisplayName("GET /api/watchlist/contains/{tmdbId} returns exists=false when not in watchlist")
        void containsCheck_notInWatchlist_returnsFalse() throws Exception {
            mockMvc.perform(get("/api/watchlist/contains/" + TMDB_ID)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists", is(false)));
        }

        @Test
        @DisplayName("GET /api/watchlist/contains/{tmdbId} returns exists=true after adding")
        void containsCheck_afterAdding_returnsTrue() throws Exception {
            // Add first
            mockMvc.perform(post("/api/watchlist")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "tmdbId", TMDB_ID,
                            "contentType", "MOVIE"
                    ))));

            // Now contains must return true
            mockMvc.perform(get("/api/watchlist/contains/" + TMDB_ID)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists", is(true)));
        }
    }

    // =========================================================================
    // REGRESSION: GET /api/watchlist → 500
    // Console shows: Failed to load resource: 500 /api/watchlist
    // =========================================================================

    @Nested
    @DisplayName("REGRESSION: GET /api/watchlist — 500")
    class GetWatchlistIntegrationTests {

        @Test
        @DisplayName("GET /api/watchlist with valid JWT must NOT return 500")
        void getWatchlist_validJwt_doesNotReturn500() throws Exception {
            mockMvc.perform(get("/api/watchlist")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(500, result.getResponse().getStatus(), "Expected non-500 but got Internal Server Error"));
        }

        @Test
        @DisplayName("GET /api/watchlist returns 200 with empty array for new user")
        void getWatchlist_newUser_returns200WithEmptyArray() throws Exception {
            mockMvc.perform(get("/api/watchlist")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("GET /api/watchlist without token returns 401 not 500")
        void getWatchlist_noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/watchlist"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // REGRESSION: UserResolverImpl — the root cause of all 500s
    // Verifies the full JWT → email extraction → DB lookup chain works
    // =========================================================================

    @Nested
    @DisplayName("REGRESSION: UserResolverImpl — JWT to userId pipeline")
    class UserResolverIntegrationTests {

        @Test
        @DisplayName("Any authenticated endpoint resolves userId without 500 — full pipeline works")
        void anyAuthenticatedEndpoint_validJwt_userResolvedWithout500() throws Exception {
            // GET /api/watchlist/stats is the simplest authenticated endpoint.
            // If UserResolverImpl is broken, this returns 500.
            // If it works, we get 200. This catches the exact bug in the logs.
            mockMvc.perform(get("/api/watchlist/stats")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total", notNullValue()));
        }

        @Test
        @DisplayName("JWT email must match a user in the DB — wrong email returns 401 not 500")
        void jwtWithUnregisteredEmail_returns401() throws Exception {
            // Token for an email that doesn't exist in the DB
            String tokenForUnknownUser = jwtService.generateToken("nobody@example.com");

            mockMvc.perform(get("/api/watchlist")
                            .header("Authorization", "Bearer " + tokenForUnknownUser))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // Bug #1: Navbar — GET /watchlist page must render with nav link present
    // =========================================================================

    @Nested
    @DisplayName("Bug #1: GET /watchlist page renders correctly")
    class WatchlistPageTests {

        @Test
        @DisplayName("GET /watchlist returns 200 OK — route is registered")
        void watchlistPage_returns200() throws Exception {
            mockMvc.perform(get("/watchlist"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /watchlist response contains 'My Watchlist' nav link")
        void watchlistPage_containsNavLink() throws Exception {
            mockMvc.perform(get("/watchlist"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString("/watchlist")));
        }

        @Test
        @DisplayName("GET /catalog response contains 'My Watchlist' nav link — Browse page navbar")
        void catalogPage_navbarContainsWatchlistLink() throws Exception {
            // This catches Bug #1: Browse page navbar missing the watchlist link
            mockMvc.perform(get("/catalog"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().string(org.hamcrest.Matchers.containsString("My Watchlist")));
        }
    }

    // =========================================================================
    // Other CRUD endpoints — ensure none of them return 500
    // =========================================================================

    @Nested
    @DisplayName("Other endpoints — must not return 500")
    class OtherEndpointTests {

        @Test
        @DisplayName("PATCH /api/watchlist/{tmdbId} updates status without 500")
        void patchStatus_validEntry_doesNotReturn500() throws Exception {
            // Add entry first
            mockMvc.perform(post("/api/watchlist")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "tmdbId", TMDB_ID,
                            "contentType", "MOVIE"
                    ))));

            mockMvc.perform(patch("/api/watchlist/" + TMDB_ID)
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("status", "WATCHING"))))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(500, result.getResponse().getStatus(), "Expected non-500 but got Internal Server Error"));
        }

        @Test
        @DisplayName("DELETE /api/watchlist/{tmdbId} removes entry without 500")
        void deleteEntry_validEntry_doesNotReturn500() throws Exception {
            // Add entry first
            mockMvc.perform(post("/api/watchlist")
                    .header("Authorization", "Bearer " + validToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "tmdbId", TMDB_ID,
                            "contentType", "MOVIE"
                    ))));

            mockMvc.perform(delete("/api/watchlist/" + TMDB_ID)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(500, result.getResponse().getStatus(), "Expected non-500 but got Internal Server Error"));
        }

        @Test
        @DisplayName("GET /api/watchlist/stats returns 200 with all fields present")
        void getStats_returnsAllFields() throws Exception {
            mockMvc.perform(get("/api/watchlist/stats")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total",      notNullValue()))
                    .andExpect(jsonPath("$.wantToWatch", notNullValue()))
                    .andExpect(jsonPath("$.watching",   notNullValue()))
                    .andExpect(jsonPath("$.completed",  notNullValue()))
                    .andExpect(jsonPath("$.totalMovies", notNullValue()))
                    .andExpect(jsonPath("$.totalSeries", notNullValue()));
        }
    }
}