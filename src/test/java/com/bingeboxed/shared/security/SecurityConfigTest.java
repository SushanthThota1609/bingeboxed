// src/test/java/com/bingeboxed/shared/security/SecurityConfigTest.java

package com.bingeboxed.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * These tests run the REAL security filter chain — no mocking.
 * If a URL returns 401 when it shouldn't (or 200 when it should be 401),
 * you'll catch it here. @WithMockUser bypasses the filter chain entirely
 * which is why your unit tests passed but the app was broken.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // --- Routes that must be publicly accessible (no JWT required) ---

    @Test
    void loginPage_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerPage_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void authLoginApi_isPubliclyAccessible() throws Exception {
        // POST with empty body — should get 400 (bad request) not 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void authRegisterApi_isPubliclyAccessible() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void publicProfileApi_isPubliclyAccessible() throws Exception {
        // Should get 404 (user doesn't exist) not 401
        mockMvc.perform(get("/api/profiles/public/nonexistentuser"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    // --- Routes that must require authentication ---

    @Test
    void catalogApi_isPubliclyAccessible_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/catalog/550").param("type", "movie"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void catalogSearchApi_isPubliclyAccessible_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/catalog/search").param("q", "batman").param("type", "movie"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void watchlistApi_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileApi_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized());
    }

    // --- 401 response format must be JSON, never a redirect ---

    @Test
    void unauthorizedResponse_isJson_notHtmlRedirect() throws Exception {
        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void unauthorizedResponse_doesNotRedirectToLogin() throws Exception {
        // Spring Security default behavior redirects to /login — this config should NOT do that
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isUnauthorized())
                // 302 redirect would mean the entry point isn't wired correctly
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(302));
    }

    // --- View routes (Thymeleaf pages) ---

    @Test
    void catalogBrowsePage_isPubliclyAccessible_withoutJwt() throws Exception {
        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk());
    }

    @Test
    void watchlistPage_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isUnauthorized());
    }

    // --- Session must be stateless ---

    @Test
    void response_doesNotSetSessionCookie() throws Exception {
        mockMvc.perform(get("/api/catalog/search").param("q", "test").param("type", "movie"))
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader("Set-Cookie");
                    // JSESSIONID being set means sessions are not stateless
                    if (setCookie != null) {
                        org.assertj.core.api.Assertions
                                .assertThat(setCookie).doesNotContain("JSESSIONID");
                    }
                });
    }

    // --- JWT in Authorization header grants access ---

    @Test
    void catalogApi_returns200_withValidJwt() throws Exception {
        String token = obtainValidToken();

        mockMvc.perform(get("/api/catalog/trending")
                        .param("type", "movie")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void invalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer this.is.not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwt_returns401() throws Exception {
        // A syntactically valid but expired token
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9" +
                ".eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDF9" +
                ".invalid_signature";

        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // --- Helper: register + login to get a real token ---

    private String obtainValidToken() throws Exception {
        String email = "sectest_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\",\"username\":\"sectest\"}"));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // parse token from {"token":"..."}
        return loginResponse.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }
}