// src/test/java/com/bingeboxed/catalog/CatalogFrontendTest.java

package com.bingeboxed.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogFrontendTest {

    @Autowired
    private MockMvc mockMvc;

    private String authCookie;

    @BeforeEach
    void loginAndGetCookie() throws Exception {
        String email = "catalogui_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\",\"username\":\"catalogui\"}"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\"}"))
                .andReturn();

        String cookie = login.getResponse().getHeader("Set-Cookie");
        if (cookie != null) {
            authCookie = cookie.split(";")[0];
        }
    }

    // --- /catalog page loads ---

    @Test
    void catalogPage_returns200_whenAuthenticated() throws Exception {
        getPage("/catalog");
    }

    @Test
    void catalogPage_isPubliclyAccessible_withoutAuth() throws Exception {
        MvcResult result = mockMvc.perform(get("/catalog")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    // --- Navbar links on /catalog ---

    @Test
    void catalogPage_hasNavLinkToProfile() throws Exception {
        String html = getPage("/catalog");
        assertThat(html)
                .as("Catalog navbar missing link to /profile")
                .containsAnyOf("href=\"/profile\"", "href='/profile'");
    }

    @Test
    void catalogPage_hasNavLinkToWatchlist() throws Exception {
        String html = getPage("/catalog");
        assertThat(html)
                .as("Catalog navbar missing link to /watchlist")
                .containsAnyOf("href=\"/watchlist\"", "href='/watchlist'");
    }

    @Test
    void catalogPage_hasLogoutOption() throws Exception {
        String html = getPage("/catalog");
        assertThat(html).containsIgnoringCase("logout");
    }

    @Test
    void catalogPage_logoutIsNotAGetHref() throws Exception {
        // common mistake: wiring logout to href="/logout" instead of a JS POST
        String html = getPage("/catalog");
        assertThat(html)
                .as("Logout should not be a plain GET href — should use fetch() POST")
                .doesNotContain("href=\"/logout\"");
    }

    // --- Catalog page content ---

    @Test
    void catalogPage_hasSearchBar() throws Exception {
        String html = getPage("/catalog");
        assertThat(html)
                .as("Catalog page missing search input")
                .containsAnyOf("type=\"search\"", "type=\"text\"", "placeholder");
    }

    @Test
    void catalogPage_hasTypeFilter() throws Exception {
        String html = getPage("/catalog");
        assertThat(html).containsIgnoringCase("movie");
        assertThat(html).containsIgnoringCase("series");
    }

    @Test
    void catalogPage_hasGenreFilter() throws Exception {
        String html = getPage("/catalog");
        assertThat(html)
                .as("Catalog page missing genre filter dropdown")
                .containsIgnoringCase("genre");
    }

    @Test
    void catalogPage_usesBingeBoxedBranding() throws Exception {
        String html = getPage("/catalog");
        assertThat(html).containsIgnoringCase("BingeBoxed");
    }

    // --- /catalog/{tmdbId} detail page ---

    @Test
    void catalogDetailPage_hasAddToWatchlistButton() throws Exception {
        // using a well-known TMDB id — if TMDB is mocked/stubbed in test profile this still checks the template
        String html = getPage("/catalog/550?type=movie");
        assertThat(html)
                .as("Detail page missing Add to Watchlist button")
                .containsAnyOf("watchlist", "Watchlist");
    }

    @Test
    void catalogDetailPage_hasNavLinkToProfile() throws Exception {
        String html = getPage("/catalog/550?type=movie");
        assertThat(html)
                .as("Detail page navbar missing link to /profile")
                .containsAnyOf("href=\"/profile\"", "href='/profile'");
    }

    @Test
    void catalogDetailPage_hasNavLinkToBrowse() throws Exception {
        String html = getPage("/catalog/550?type=movie");
        assertThat(html)
                .as("Detail page navbar missing link back to /catalog")
                .containsAnyOf("href=\"/catalog\"", "href='/catalog'");
    }

    // --- No sensitive data in rendered HTML ---

    @Test
    void catalogPage_doesNotLeakJwtInHtml() throws Exception {
        String html = getPage("/catalog");
        // JWT tokens always start with eyJ
        assertThat(html)
                .as("JWT token should never appear in server-rendered HTML")
                .doesNotContain("eyJ");
    }

    // --- Helper ---

    private String getPage(String path) throws Exception {
        var request = get(path);
        if (authCookie != null) {
            request = request.header("Cookie", authCookie);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}