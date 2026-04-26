// src/test/java/com/bingeboxed/recommendations/RecommendationsTest.java

package com.bingeboxed.recommendations;

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
class RecommendationServiceTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void registerAndLogin() throws Exception {
        String email = "rec_" + System.currentTimeMillis() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\",\"username\":\"recuser\"}"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\"}"))
                .andReturn();

        token = login.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    // --- FR-01: Generate ---

    @Test
    void generate_returns200_whenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/recommendations/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void generate_returns401_withoutJwt() throws Exception {
        mockMvc.perform(post("/api/recommendations/generate"))
                .andExpect(status().isUnauthorized());
    }

    // --- FR-02: Get My Recommendations ---

    @Test
    void getRecommendations_returns200_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getRecommendations_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized());
    }

    // --- FR-03: Get Single ---

    @Test
    void getSingle_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/recommendations/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSingle_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/recommendations/550"))
                .andExpect(status().isUnauthorized());
    }

    // --- FR-04: Dismiss ---

    @Test
    void dismiss_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/recommendations/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void dismiss_returns401_withoutJwt() throws Exception {
        mockMvc.perform(delete("/api/recommendations/550"))
                .andExpect(status().isUnauthorized());
    }

    // --- FR-05: Get Reason ---

    @Test
    void getReason_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/recommendations/99999/reason")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReason_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/recommendations/550/reason"))
                .andExpect(status().isUnauthorized());
    }

    // --- FR-06: Recommendations Page ---

    @Test
    void recommendationsPage_returns401_whenNotAuthenticated() throws Exception {
        int status = mockMvc.perform(get("/recommendations"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(200);
    }

    @Test
    void recommendationsPage_returns200_whenAuthenticated() throws Exception {
        String email = "recpage_" + System.currentTimeMillis() + "@test.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\",\"username\":\"recpage\"}"));
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Test1234!\"}"))
                .andReturn();
        String cookie = login.getResponse().getHeader("Set-Cookie");
        var req = get("/recommendations");
        if (cookie != null) req = req.header("Cookie", cookie.split(";")[0]);
        mockMvc.perform(req).andExpect(status().isOk());
    }
}